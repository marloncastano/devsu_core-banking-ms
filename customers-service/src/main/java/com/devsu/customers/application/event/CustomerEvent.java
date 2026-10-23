package com.devsu.customers.application.event;

import java.time.Instant;
import java.util.UUID;

import com.devsu.customers.domain.model.Customer;

/**
 * Notifies other services that a customer changed. It carries the state they need, so consumers never have to
 * call back this service (event-carried state transfer).
 */
public record CustomerEvent(
		UUID eventId,
		Type type,
		UUID customerId,
		String name,
		boolean active,
		Instant occurredAt) {

	public enum Type {
		CREATED,
		UPDATED,
		DEACTIVATED
	}

	public static CustomerEvent of(Type type, Customer customer, Instant occurredAt) {
		return new CustomerEvent(UUID.randomUUID(), type, customer.getCustomerId(), customer.getPersonalData().name(),
				customer.isActive(), occurredAt);
	}
}
