package com.devsu.accounts.domain.model;

import java.util.UUID;

/**
 * Local copy of the customer data this service needs. The customer itself is owned by customers-service.
 */
public record CustomerReference(UUID customerId, String name, boolean active) {
}
