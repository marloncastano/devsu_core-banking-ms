package com.devsu.customers.infrastructure.adapter.out.messaging;

import java.time.Instant;
import java.util.UUID;

import com.devsu.customers.application.event.CustomerEvent;

/**
 * JSON contract of the customer events published to RabbitMQ. Never include sensitive data such as the password.
 */
record CustomerEventMessage(
		UUID eventId,
		String eventType,
		UUID customerId,
		String name,
		boolean active,
		Instant occurredAt) {

	static CustomerEventMessage from(CustomerEvent event) {
		return new CustomerEventMessage(event.eventId(), event.type().name(), event.customerId(), event.name(),
				event.active(), event.occurredAt());
	}
}
