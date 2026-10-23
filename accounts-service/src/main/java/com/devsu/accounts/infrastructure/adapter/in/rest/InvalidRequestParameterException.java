package com.devsu.accounts.infrastructure.adapter.in.rest;

/**
 * A query parameter has a format the API cannot understand.
 */
class InvalidRequestParameterException extends RuntimeException {

	private final String parameter;

	InvalidRequestParameterException(String parameter, String message) {
		super(message);
		this.parameter = parameter;
	}

	String getParameter() {
		return parameter;
	}
}
