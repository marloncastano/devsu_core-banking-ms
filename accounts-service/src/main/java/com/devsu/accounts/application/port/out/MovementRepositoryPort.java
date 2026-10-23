package com.devsu.accounts.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.domain.model.Movement;

public interface MovementRepositoryPort {

	Movement save(Movement movement);

	Optional<Movement> findByMovementId(UUID movementId);

	/**
	 * Most recent first.
	 *
	 * @param accountNumber optional filter; {@code null} returns the movements of every account.
	 */
	PageResult<Movement> findAll(PageQuery query, String accountNumber);
}
