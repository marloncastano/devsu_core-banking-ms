package com.devsu.accounts.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.application.port.in.QueryMovementsUseCase;
import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.MovementNotFoundException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;

@Service
@Transactional
public class MovementService implements RegisterMovementUseCase, QueryMovementsUseCase {

	private final AccountRepositoryPort accountRepository;
	private final CustomerReferenceRepositoryPort customerReferenceRepository;
	private final MovementRepositoryPort movementRepository;
	private final Clock clock;

	public MovementService(AccountRepositoryPort accountRepository,
			CustomerReferenceRepositoryPort customerReferenceRepository, MovementRepositoryPort movementRepository,
			Clock clock) {
		this.accountRepository = accountRepository;
		this.customerReferenceRepository = customerReferenceRepository;
		this.movementRepository = movementRepository;
		this.clock = clock;
	}

	/**
	 * The account row stays locked until the transaction ends, so concurrent movements on the same account are
	 * applied one after another and the balance check always sees the latest balance.
	 */
	@Override
	public Movement register(RegisterMovementCommand command) {
		Account account = accountRepository.findByAccountNumberForUpdate(command.accountNumber())
				.orElseThrow(() -> AccountNotAvailableException.notFound(command.accountNumber()));
		boolean customerActive = customerReferenceRepository.findById(account.getCustomerId())
				.map(CustomerReference::active)
				.orElse(false);
		if (!customerActive) {
			throw CustomerNotAvailableException.inactive(account.getCustomerId());
		}
		Movement movement = account.registerMovement(command.amount(), clock.instant());
		accountRepository.save(account);
		return movementRepository.save(movement);
	}

	@Override
	@Transactional(readOnly = true)
	public Movement getByMovementId(UUID movementId) {
		return movementRepository.findByMovementId(movementId)
				.orElseThrow(() -> new MovementNotFoundException(movementId));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Movement> list(PageQuery query, String accountNumber) {
		return movementRepository.findAll(query, accountNumber);
	}
}
