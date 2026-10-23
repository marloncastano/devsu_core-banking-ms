package com.devsu.customers.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ topology owned by this service. Consumers declare and bind their own queues.
 */
@Configuration(proxyBeanMethods = false)
public class CustomerEventsTopology {

	public static final String EXCHANGE = "customers.events";

	@Bean
	TopicExchange customersEventsExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
		return new JacksonJsonMessageConverter(jsonMapper);
	}
}
