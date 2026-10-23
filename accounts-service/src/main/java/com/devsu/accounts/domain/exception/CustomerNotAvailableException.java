package com.devsu.accounts.domain.exception;

import java.util.UUID;

/**
 * The customer referenced by an operation does not exist or cannot operate.
 */
public class CustomerNotAvailableException extends RuntimeException {

	private CustomerNotAvailableException(String message) {
		super(message);
	}

	public static CustomerNotAvailableException notFound(UUID customerId) {
		return new CustomerNotAvailableException("Customer " + customerId + " does not exist");
	}

	public static CustomerNotAvailableException inactive(UUID customerId) {
		return new CustomerNotAvailableException("Customer " + customerId + " is inactive");
	}
}
