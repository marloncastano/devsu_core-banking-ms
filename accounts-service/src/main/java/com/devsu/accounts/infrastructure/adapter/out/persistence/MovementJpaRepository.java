package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The account is fetched in the same query ({@link EntityGraph}) to avoid one extra query per movement (N+1).
 */
interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {

	@EntityGraph(attributePaths = "account")
	Optional<MovementEntity> findByMovementId(UUID movementId);

	@EntityGraph(attributePaths = "account")
	Optional<MovementEntity> findByIdempotencyKey(String idempotencyKey);

	@EntityGraph(attributePaths = "account")
	Page<MovementEntity> findAllBy(Pageable pageable);

	/**
	 * Uses the (account_id, occurred_at) index; the account is fetched in the same query.
	 */
	@Query("""
			SELECT m FROM MovementEntity m JOIN FETCH m.account a
			WHERE a.customerId = :customerId AND m.occurredAt >= :from AND m.occurredAt < :toExclusive
			ORDER BY a.id, m.occurredAt, m.id
			""")
	List<MovementEntity> findByCustomerBetween(@Param("customerId") UUID customerId, @Param("from") Instant from,
			@Param("toExclusive") Instant toExclusive);

	@EntityGraph(attributePaths = "account")
	Page<MovementEntity> findByAccountAccountNumber(String accountNumber, Pageable pageable);
}
