package com.devsu.accounts.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.application.port.in.OpenAccountCommand;
import com.devsu.accounts.application.port.out.AccountNumberGeneratorPort;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.exception.AccountNotFoundException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.CustomerReference;

@Service
@Transactional
public class AccountService implements ManageAccountUseCase {

	private final AccountRepositoryPort accountRepository;
	private final CustomerReferenceRepositoryPort customerReferenceRepository;
	private final AccountNumberGeneratorPort accountNumberGenerator;

	public AccountService(AccountRepositoryPort accountRepository,
			CustomerReferenceRepositoryPort customerReferenceRepository,
			AccountNumberGeneratorPort accountNumberGenerator) {
		this.accountRepository = accountRepository;
		this.customerReferenceRepository = customerReferenceRepository;
		this.accountNumberGenerator = accountNumberGenerator;
	}

	@Override
	public Account open(OpenAccountCommand command) {
		CustomerReference customer = customerReferenceRepository.findById(command.customerId())
				.orElseThrow(() -> CustomerNotAvailableException.notFound(command.customerId()));
		Account account = Account.open(accountNumberGenerator.next(), command.accountType(), command.initialBalance(),
				customer);
		return accountRepository.save(account);
	}

	@Override
	@Transactional(readOnly = true)
	public Account getByAccountNumber(String accountNumber) {
		return accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new AccountNotFoundException(accountNumber));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Account> list(PageQuery query, UUID customerId) {
		return accountRepository.findAll(query, customerId);
	}
}
