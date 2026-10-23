package com.devsu.accounts.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Latest known state of a customer, as announced by customers-service.
 */
public record SyncCustomerCommand(UUID customerId, String name, boolean active, Instant occurredAt) {
}
