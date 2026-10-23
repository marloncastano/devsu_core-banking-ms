package com.devsu.accounts.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON contract of the customer events published by customers-service. Unknown fields are ignored so the producer
 * can add new ones without breaking this consumer.
 */
record CustomerEventMessage(
		UUID eventId,
		String eventType,
		UUID customerId,
		String name,
		Boolean active,
		Instant occurredAt) {

	boolean isComplete() {
		return eventId != null && customerId != null && name != null && !name.isBlank() && active != null
				&& occurredAt != null;
	}
}
