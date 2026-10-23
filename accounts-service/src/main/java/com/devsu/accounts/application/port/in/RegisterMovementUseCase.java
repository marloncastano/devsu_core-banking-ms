package com.devsu.accounts.application.port.in;

public interface RegisterMovementUseCase {

	/**
	 * Applies the movement, or returns the original one when the same Idempotency-Key was already processed.
	 */
	MovementRegistration register(RegisterMovementCommand command);
}
