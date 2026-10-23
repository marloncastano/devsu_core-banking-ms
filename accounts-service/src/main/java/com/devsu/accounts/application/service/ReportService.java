package com.devsu.accounts.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.accounts.application.port.in.GenerateStatementUseCase;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.application.report.AccountStatement;
import com.devsu.accounts.application.report.StatementQuery;
import com.devsu.accounts.domain.exception.CustomerNotFoundException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;

@Service
@Transactional(readOnly = true)
public class ReportService implements GenerateStatementUseCase {

	private final CustomerReferenceRepositoryPort customerReferenceRepository;
	private final AccountRepositoryPort accountRepository;
	private final MovementRepositoryPort movementRepository;
	private final Clock clock;

	public ReportService(CustomerReferenceRepositoryPort customerReferenceRepository,
			AccountRepositoryPort accountRepository, MovementRepositoryPort movementRepository, Clock clock) {
		this.customerReferenceRepository = customerReferenceRepository;
		this.accountRepository = accountRepository;
		this.movementRepository = movementRepository;
		this.clock = clock;
	}

	/**
	 * Two queries whatever the number of accounts or movements: one for the accounts and one for all their movements
	 * in the period. Dates are whole days in the bank's time zone.
	 */
	@Override
	public AccountStatement generate(StatementQuery query) {
		CustomerReference customer = customerReferenceRepository.findById(query.customerId())
				.orElseThrow(() -> new CustomerNotFoundException(query.customerId()));
		ZoneId zone = clock.getZone();
		Instant from = query.from().atStartOfDay(zone).toInstant();
		Instant toExclusive = query.to().plusDays(1).atStartOfDay(zone).toInstant();

		List<Account> accounts = accountRepository.findAllByCustomerId(query.customerId());
		Map<String, List<Movement>> movementsByAccount = movementRepository
				.findByCustomerBetween(query.customerId(), from, toExclusive).stream()
				.collect(Collectors.groupingBy(Movement::accountNumber, LinkedHashMap::new, Collectors.toList()));

		List<AccountStatement.Entry> entries = accounts.stream()
				.map(account -> new AccountStatement.Entry(account,
						movementsByAccount.getOrDefault(account.getAccountNumber(), List.of())))
				.toList();
		return new AccountStatement(customer, query.from(), query.to(), entries);
	}
}
