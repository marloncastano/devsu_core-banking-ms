package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsu.accounts.application.port.in.GenerateStatementUseCase;
import com.devsu.accounts.application.report.AccountStatement;
import com.devsu.accounts.application.report.StatementQuery;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.AccountStatementResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * F4. The path and the query parameter names ({@code fecha}, {@code cliente}) are required by the specification.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReportController {

	static final String DATE_RANGE_FORMAT = "yyyy-MM-dd,yyyy-MM-dd";

	private final GenerateStatementUseCase generateStatement;
	private final Clock clock;

	public ReportController(GenerateStatementUseCase generateStatement, Clock clock) {
		this.generateStatement = generateStatement;
		this.clock = clock;
	}

	/**
	 * Example: {@code /api/reportes?fecha=2026-10-01,2026-10-31&cliente=7a7043f4-...}. Both dates are inclusive.
	 */
	@GetMapping
	@Operation(summary = "Account statement of a customer for a date range")
	public AccountStatementResponse accountStatement(
			@Parameter(description = "Inclusive date range: start and end date separated by a comma (yyyy-MM-dd,yyyy-MM-dd)",
					example = "2026-10-01,2026-10-31")
			@RequestParam("fecha") String dateRange,
			@Parameter(description = "customerId (UUID) returned by customers-service",
					example = "7a7043f4-d803-4a06-961b-efe46efa60de")
			@RequestParam("cliente") UUID customerId) {
		LocalDate[] range = parseDateRange(dateRange);
		AccountStatement statement = generateStatement.generate(new StatementQuery(customerId, range[0], range[1]));
		return toResponse(statement);
	}

	private static LocalDate[] parseDateRange(String value) {
		String[] parts = value.split(",", -1);
		if (parts.length != 2) {
			throw new InvalidRequestParameterException("fecha", "must have the format " + DATE_RANGE_FORMAT);
		}
		try {
			return new LocalDate[] { LocalDate.parse(parts[0].strip()), LocalDate.parse(parts[1].strip()) };
		} catch (DateTimeParseException ex) {
			throw new InvalidRequestParameterException("fecha", "must have the format " + DATE_RANGE_FORMAT);
		}
	}

	private AccountStatementResponse toResponse(AccountStatement statement) {
		return new AccountStatementResponse(statement.customer().customerId(), statement.customer().name(),
				statement.from(), statement.to(), statement.accounts().stream().map(this::toEntry).toList());
	}

	private AccountStatementResponse.AccountEntry toEntry(AccountStatement.Entry entry) {
		Account account = entry.account();
		return new AccountStatementResponse.AccountEntry(account.getAccountNumber(), account.getAccountType(),
				account.getInitialBalance(), account.getAvailableBalance(), account.isActive(), entry.totalCredits(),
				entry.totalDebits(), entry.movements().stream().map(this::toMovementEntry).toList());
	}

	private AccountStatementResponse.MovementEntry toMovementEntry(Movement movement) {
		return new AccountStatementResponse.MovementEntry(LocalDate.ofInstant(movement.occurredAt(), clock.getZone()),
				movement.occurredAt(), movement.type(), movement.amount(), movement.balance());
	}
}
