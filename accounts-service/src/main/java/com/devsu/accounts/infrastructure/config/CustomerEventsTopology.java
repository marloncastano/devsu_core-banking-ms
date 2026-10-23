package com.devsu.accounts.infrastructure.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Queue through which this service receives customer events, plus its dead letter queue for messages that keep
 * failing. The exchange is also declared here so the start-up order of the services does not matter.
 */
@Configuration(proxyBeanMethods = false)
public class CustomerEventsTopology {

	public static final String EXCHANGE = "customers.events";
	public static final String QUEUE = "accounts.customer-events";
	static final String ROUTING_PATTERN = "customer.*";
	static final String DEAD_LETTER_EXCHANGE = "accounts.customer-events.dlx";
	static final String DEAD_LETTER_QUEUE = "accounts.customer-events.dlq";

	@Bean
	TopicExchange customersEventsExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	Queue customerEventsQueue() {
		return QueueBuilder.durable(QUEUE)
				.deadLetterExchange(DEAD_LETTER_EXCHANGE)
				.deadLetterRoutingKey(DEAD_LETTER_QUEUE)
				.build();
	}

	@Bean
	Binding customerEventsBinding(Queue customerEventsQueue, TopicExchange customersEventsExchange) {
		return BindingBuilder.bind(customerEventsQueue).to(customersEventsExchange).with(ROUTING_PATTERN);
	}

	@Bean
	DirectExchange customerEventsDeadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
	}

	@Bean
	Queue customerEventsDeadLetterQueue() {
		return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
	}

	@Bean
	Binding customerEventsDeadLetterBinding(Queue customerEventsDeadLetterQueue,
			DirectExchange customerEventsDeadLetterExchange) {
		return BindingBuilder.bind(customerEventsDeadLetterQueue).to(customerEventsDeadLetterExchange)
				.with(DEAD_LETTER_QUEUE);
	}

	/**
	 * Messages that can never be processed (malformed or explicitly rejected) go straight to the dead letter queue
	 * instead of wasting retries.
	 */
	@Bean
	RabbitListenerRetrySettingsCustomizer skipRetryForUnprocessableMessages() {
		return settings -> settings.setExceptionPredicate(CustomerEventsTopology::isRetryable);
	}

	static boolean isRetryable(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof AmqpRejectAndDontRequeueException || cause instanceof MessageConversionException) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts by the listener parameter type instead of the producer's class name header, so both services stay
	 * decoupled; tolerates new fields added by the producer.
	 */
	@Bean
	MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
		JsonMapper tolerantMapper = jsonMapper.rebuild()
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(tolerantMapper);
		converter.setAlwaysConvertToInferredType(true);
		return converter;
	}
}
