package com.devsu.accounts.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

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
		movementJpaRepository.save(entity);
		return movement;
	}
}
