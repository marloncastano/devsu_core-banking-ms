package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CustomerReferenceJpaRepository extends JpaRepository<CustomerReferenceEntity, UUID> {

	/**
	 * Single atomic statement, so concurrent or redelivered events cannot overwrite a newer state.
	 */
	@Modifying
	@Query(value = """
			INSERT INTO customer_ref (customer_id, name, active, last_event_at, updated_at)
			VALUES (:customerId, :name, :active, :occurredAt, now())
			ON CONFLICT (customer_id) DO UPDATE
			   SET name = EXCLUDED.name,
			       active = EXCLUDED.active,
			       last_event_at = EXCLUDED.last_event_at,
			       updated_at = now()
			 WHERE customer_ref.last_event_at IS NULL
			    OR customer_ref.last_event_at <= EXCLUDED.last_event_at
			""", nativeQuery = true)
	int upsertIfNotOlder(@Param("customerId") UUID customerId, @Param("name") String name,
			@Param("active") boolean active, @Param("occurredAt") Instant occurredAt);
}
