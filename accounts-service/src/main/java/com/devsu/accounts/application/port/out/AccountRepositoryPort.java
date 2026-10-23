package com.devsu.accounts.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.domain.model.Account;

public interface AccountRepositoryPort {

	Account save(Account account);

	Optional<Account> findByAccountNumber(String accountNumber);

	/**
	 * @param customerId optional filter; {@code null} returns the accounts of every customer.
	 */
	PageResult<Account> findAll(PageQuery query, UUID customerId);
}
