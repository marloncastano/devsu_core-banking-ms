package com.devsu.accounts.application.port.in;

import java.util.UUID;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.domain.model.Movement;

public interface QueryMovementsUseCase {

	Movement getByMovementId(UUID movementId);

	/**
	 * Most recent first.
	 *
	 * @param accountNumber optional filter; {@code null} lists the movements of every account.
	 */
	PageResult<Movement> list(PageQuery query, String accountNumber);
}
