package com.devsu.customers.infrastructure.adapter.out.messaging;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.devsu.customers.application.event.CustomerEvent;
import com.devsu.customers.application.port.out.CustomerEventPublisherPort;
import com.devsu.customers.infrastructure.config.CustomerEventsTopology;

/**
 * Sends customer events to RabbitMQ only after the database transaction commits.
 * <p>
 * Known limitation: if the broker is down right after the commit the event is lost. A transactional outbox would
 * close that gap and is documented as a future improvement.
 */
@Component
class RabbitCustomerEventPublisher implements CustomerEventPublisherPort {

	private static final Logger log = LoggerFactory.getLogger(RabbitCustomerEventPublisher.class);

	private final ApplicationEventPublisher applicationEventPublisher;
	private final RabbitTemplate rabbitTemplate;

	RabbitCustomerEventPublisher(ApplicationEventPublisher applicationEventPublisher, RabbitTemplate rabbitTemplate) {
		this.applicationEventPublisher = applicationEventPublisher;
		this.rabbitTemplate = rabbitTemplate;
		rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
			if (!ack) {
				log.error("Broker did not confirm customer event {}: {}", correlationId(correlation), cause);
			}
		});
		rabbitTemplate.setReturnsCallback(returned -> log.error("Customer event {} was not routed to any queue: {}",
				returned.getMessage().getMessageProperties().getMessageId(), returned.getReplyText()));
	}

	@Override
	public void publish(CustomerEvent event) {
		applicationEventPublisher.publishEvent(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void sendAfterCommit(CustomerEvent event) {
		String routingKey = "customer." + event.type().name().toLowerCase(Locale.ROOT);
		rabbitTemplate.convertAndSend(CustomerEventsTopology.EXCHANGE, routingKey, CustomerEventMessage.from(event),
				message -> {
					message.getMessageProperties().setMessageId(event.eventId().toString());
					return message;
				}, new CorrelationData(event.eventId().toString()));
		log.info("Published {} for customer {}", routingKey, event.customerId());
	}

	private static String correlationId(CorrelationData correlation) {
		return correlation != null ? correlation.getId() : "unknown";
	}
}
