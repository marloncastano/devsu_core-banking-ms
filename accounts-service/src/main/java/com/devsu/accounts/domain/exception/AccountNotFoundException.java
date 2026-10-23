package com.devsu.accounts.domain.exception;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(String accountNumber) {
		super("Account not found with number " + accountNumber);
	}
}
