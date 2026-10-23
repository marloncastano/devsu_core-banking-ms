package com.devsu.accounts.domain.exception;

/**
 * The account referenced by an operation does not exist or cannot operate.
 */
public class AccountNotAvailableException extends RuntimeException {

	private AccountNotAvailableException(String message) {
		super(message);
	}

	public static AccountNotAvailableException notFound(String accountNumber) {
		return new AccountNotAvailableException("Account " + accountNumber + " does not exist");
	}

	public static AccountNotAvailableException inactive(String accountNumber) {
		return new AccountNotAvailableException("Account " + accountNumber + " is inactive");
	}
}
