package com.devsu.accounts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry. {@code balance} is the available balance of the account right after this movement.
 */
public record Movement(
		UUID movementId,
		Long accountId,
		String accountNumber,
		MovementType type,
		BigDecimal amount,
		BigDecimal balance,
		Instant occurredAt) {
}
