package com.devsu.accounts.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry. {@code balance} is the available balance of the account right after this movement.
 *
 * @param idempotencyKey key supplied by the client to make retries safe; {@code null} when none was sent.
 */
public record Movement(
		UUID movementId,
		Long accountId,
		String accountNumber,
		MovementType type,
		BigDecimal amount,
		BigDecimal balance,
		Instant occurredAt,
		String idempotencyKey) {

	public Movement withIdempotencyKey(String key) {
		return new Movement(movementId, accountId, accountNumber, type, amount, balance, occurredAt, key);
	}

	/**
	 * Whether a request for {@code requestedAmount} on {@code requestedAccountNumber} is the one that produced this
	 * movement, i.e. a legitimate retry.
	 */
	public boolean matches(String requestedAccountNumber, BigDecimal requestedAmount) {
		return accountNumber.equals(requestedAccountNumber) && amount.compareTo(requestedAmount) == 0;
	}
}
