package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.devsu.accounts.domain.model.MovementType;

/**
 * @param balance available balance of the account right after this movement.
 */
public record MovementResponse(
		UUID movementId,
		String accountNumber,
		MovementType movementType,
		BigDecimal amount,
		BigDecimal balance,
		Instant occurredAt) {
}
