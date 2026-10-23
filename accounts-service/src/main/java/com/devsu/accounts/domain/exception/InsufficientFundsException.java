package com.devsu.accounts.domain.exception;

/**
 * A withdrawal would leave the account with a negative balance.
 */
public class InsufficientFundsException extends RuntimeException {

	/**
	 * Exact message required by the specification.
	 */
	public static final String MESSAGE = "Saldo no disponible";

	public InsufficientFundsException() {
		super(MESSAGE);
	}
}
