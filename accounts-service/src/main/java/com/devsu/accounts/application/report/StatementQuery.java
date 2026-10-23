package com.devsu.accounts.application.report;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.devsu.accounts.domain.exception.DomainValidationException;

/**
 * Account statement request. Both dates are inclusive.
 */
public record StatementQuery(UUID customerId, LocalDate from, LocalDate to) {

	/**
	 * Bounds the amount of data a single report can load.
	 */
	public static final int MAX_DAYS = 366;

	public StatementQuery {
		if (customerId == null || from == null || to == null) {
			throw new DomainValidationException("Customer and date range are required");
		}
		if (from.isAfter(to)) {
			throw new DomainValidationException("The start date must not be after the end date");
		}
		if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
			throw new DomainValidationException("The date range cannot exceed " + MAX_DAYS + " days");
		}
	}
}
