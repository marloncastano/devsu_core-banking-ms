package com.devsu.accounts.domain.exception;

/**
 * The same Idempotency-Key was sent with a different request, which usually means a client bug.
 */
public class IdempotencyKeyReusedException extends RuntimeException {

	public IdempotencyKeyReusedException(String key) {
		super("Idempotency-Key " + key + " was already used for a different movement");
	}
}
