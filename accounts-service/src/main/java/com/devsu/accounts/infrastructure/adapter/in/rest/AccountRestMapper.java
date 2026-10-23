package com.devsu.accounts.infrastructure.adapter.in.rest;

import com.devsu.accounts.application.port.in.OpenAccountCommand;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.AccountResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.CreateAccountRequest;

final class AccountRestMapper {

	private AccountRestMapper() {
	}

	static OpenAccountCommand toCommand(CreateAccountRequest request) {
		return new OpenAccountCommand(request.accountType(), request.initialBalance(), request.customerId());
	}

	static AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getAccountNumber(), account.getAccountType(), account.getInitialBalance(),
				account.getAvailableBalance(), account.isActive(), account.getCustomerId());
	}
}
