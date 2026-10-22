package com.devsu.customers.infrastructure.adapter.in.rest.dto;

final class CustomerValidation {

	static final String IDENTIFICATION = "^[A-Za-z0-9-]{5,20}$";
	static final String IDENTIFICATION_MESSAGE = "must contain between 5 and 20 alphanumeric characters";
	static final String PHONE = "^\\+?[0-9]{7,15}$";
	static final String PHONE_MESSAGE = "must contain between 7 and 15 digits, optionally prefixed with +";
	static final int PASSWORD_MIN_LENGTH = 4;
	static final String PASSWORD_MIN_MESSAGE = "must have at least 4 characters";
	/** BCrypt accepts at most 72 bytes. */
	static final int PASSWORD_MAX_BYTES = 72;

	private CustomerValidation() {
	}
}
