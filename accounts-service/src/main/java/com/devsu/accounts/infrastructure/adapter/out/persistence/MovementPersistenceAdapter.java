package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.domain.model.Movement;

@Component
class MovementPersistenceAdapter implements MovementRepositoryPort {

	private final MovementJpaRepository movementJpaRepository;
	private final AccountJpaRepository accountJpaRepository;

	MovementPersistenceAdapter(MovementJpaRepository movementJpaRepository, AccountJpaRepository accountJpaRepository) {
		this.movementJpaRepository = movementJpaRepository;
		this.accountJpaRepository = accountJpaRepository;
	}

	@Override
	public Movement save(Movement movement) {
		MovementEntity entity = new MovementEntity();
		entity.setMovementId(movement.movementId());
		entity.setAccount(accountJpaRepository.getReferenceById(movement.accountId()));
		entity.setMovementType(movement.type());
		entity.setAmount(movement.amount());
		entity.setBalance(movement.balance());
		entity.setOccurredAt(movement.occurredAt());
		entity.setIdempotencyKey(movement.idempotencyKey());
		movementJpaRepository.save(entity);
		return movement;
	}

	@Override
	public Optional<Movement> findByMovementId(UUID movementId) {
		return movementJpaRepository.findByMovementId(movementId).map(MovementPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<Movement> findByIdempotencyKey(String idempotencyKey) {
		return movementJpaRepository.findByIdempotencyKey(idempotencyKey).map(MovementPersistenceAdapter::toDomain);
	}

	@Override
	public List<Movement> findByCustomerBetween(UUID customerId, Instant from, Instant toExclusive) {
		return movementJpaRepository.findByCustomerBetween(customerId, from, toExclusive).stream()
				.map(MovementPersistenceAdapter::toDomain).toList();
	}

	/**
	 * Most recent first; the technical id breaks ties so pages stay stable.
	 */
	@Override
	public PageResult<Movement> findAll(PageQuery query, String accountNumber) {
		Pageable pageable = PageRequest.of(query.page(), query.size(),
				Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
		Page<MovementEntity> page = accountNumber == null
				? movementJpaRepository.findAllBy(pageable)
				: movementJpaRepository.findByAccountAccountNumber(accountNumber, pageable);
		return new PageResult<>(page.getContent(), query.page(), query.size(), page.getTotalElements())
				.map(MovementPersistenceAdapter::toDomain);
	}

	private static Movement toDomain(MovementEntity entity) {
		AccountEntity account = entity.getAccount();
		return new Movement(entity.getMovementId(), account.getId(), account.getAccountNumber(),
				entity.getMovementType(), entity.getAmount(), entity.getBalance(), entity.getOccurredAt(),
				entity.getIdempotencyKey());
	}
}
