package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.MovementType;

/**
 * Account statement grouped by account: balances of each account plus the detail of its movements in the period.
 */
public record AccountStatementResponse(
		UUID customerId,
		String customerName,
		LocalDate from,
		LocalDate to,
		List<AccountEntry> accounts) {

	public record AccountEntry(
			String accountNumber,
			AccountType accountType,
			BigDecimal initialBalance,
			BigDecimal availableBalance,
			boolean active,
			BigDecimal totalCredits,
			BigDecimal totalDebits,
			List<MovementEntry> movements) {
	}

	/**
	 * @param date    day of the movement in the bank's time zone.
	 * @param balance available balance right after the movement.
	 */
	public record MovementEntry(
			LocalDate date,
			Instant occurredAt,
			MovementType movementType,
			BigDecimal amount,
			BigDecimal balance) {
	}
}
