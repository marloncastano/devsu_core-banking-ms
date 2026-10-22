package com.devsu.customers.domain.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(UUID customerId) {
		super("Customer not found with customerId " + customerId);
	}
}
