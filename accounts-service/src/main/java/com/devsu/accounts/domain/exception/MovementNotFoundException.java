package com.devsu.accounts.domain.exception;

import java.util.UUID;

public class MovementNotFoundException extends RuntimeException {

	public MovementNotFoundException(UUID movementId) {
		super("Movement not found with id " + movementId);
	}
}
