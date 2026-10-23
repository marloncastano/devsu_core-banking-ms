package com.devsu.accounts.application.port.in;

import java.util.UUID;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.domain.model.Account;

public interface ManageAccountUseCase {

	Account open(OpenAccountCommand command);

	Account getByAccountNumber(String accountNumber);

	/**
	 * @param customerId optional filter; {@code null} lists the accounts of every customer.
	 */
	PageResult<Account> list(PageQuery query, UUID customerId);
}
