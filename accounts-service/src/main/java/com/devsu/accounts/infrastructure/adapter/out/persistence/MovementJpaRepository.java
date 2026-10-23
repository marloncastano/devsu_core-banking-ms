package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The account is fetched in the same query ({@link EntityGraph}) to avoid one extra query per movement (N+1).
 */
interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {

	@EntityGraph(attributePaths = "account")
	Optional<MovementEntity> findByMovementId(UUID movementId);

	@EntityGraph(attributePaths = "account")
	Page<MovementEntity> findAllBy(Pageable pageable);

	@EntityGraph(attributePaths = "account")
	Page<MovementEntity> findByAccountAccountNumber(String accountNumber, Pageable pageable);
}
