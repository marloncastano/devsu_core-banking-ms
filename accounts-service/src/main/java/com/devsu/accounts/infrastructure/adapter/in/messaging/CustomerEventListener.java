package com.devsu.accounts.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.devsu.accounts.application.port.in.SyncCustomerCommand;
import com.devsu.accounts.application.port.in.SyncCustomerUseCase;
import com.devsu.accounts.infrastructure.config.CustomerEventsTopology;

/**
 * Keeps the local copy of customers up to date from the events published by customers-service.
 */
@Component
class CustomerEventListener {

	private static final Logger log = LoggerFactory.getLogger(CustomerEventListener.class);

	private final SyncCustomerUseCase syncCustomer;

	CustomerEventListener(SyncCustomerUseCase syncCustomer) {
		this.syncCustomer = syncCustomer;
	}

	@RabbitListener(queues = CustomerEventsTopology.QUEUE)
	void onCustomerEvent(CustomerEventMessage message) {
		if (message == null || !message.isComplete()) {
			// Retrying cannot fix a malformed message: send it straight to the dead letter queue.
			throw new AmqpRejectAndDontRequeueException("Incomplete customer event: " + message);
		}
		log.info("Received {} for customer {}", message.eventType(), message.customerId());
		syncCustomer.sync(new SyncCustomerCommand(message.customerId(), message.name(), message.active(),
				message.occurredAt()));
	}
}
