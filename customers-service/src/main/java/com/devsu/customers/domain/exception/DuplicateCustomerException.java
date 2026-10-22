package com.devsu.customers.domain.exception;

public class DuplicateCustomerException extends RuntimeException {

	private DuplicateCustomerException(String message) {
		super(message);
	}

	public static DuplicateCustomerException byIdentification(String identification) {
		return new DuplicateCustomerException("A person with identification " + identification + " already exists");
	}
}
