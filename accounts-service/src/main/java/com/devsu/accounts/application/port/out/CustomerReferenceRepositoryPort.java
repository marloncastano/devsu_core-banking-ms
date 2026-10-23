package com.devsu.accounts.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.devsu.accounts.domain.model.CustomerReference;

public interface CustomerReferenceRepositoryPort {

	Optional<CustomerReference> findById(UUID customerId);

	/**
	 * Inserts or updates the customer only if {@code occurredAt} is not older than the last change applied.
	 *
	 * @return {@code false} when the change was ignored because a newer one had already been applied.
	 */
	boolean saveIfNotOlder(CustomerReference customer, Instant occurredAt);
}
