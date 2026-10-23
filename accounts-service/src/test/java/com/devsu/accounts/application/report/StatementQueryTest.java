package com.devsu.accounts.application.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsu.accounts.domain.exception.DomainValidationException;

class StatementQueryTest {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Test
	@DisplayName("a single day and a full year are valid ranges")
	void acceptsValidRanges() {
		LocalDate day = LocalDate.of(2026, 10, 22);

		assertThatCode(() -> new StatementQuery(CUSTOMER_ID, day, day)).doesNotThrowAnyException();
		assertThatCode(() -> new StatementQuery(CUSTOMER_ID, day, day.plusDays(StatementQuery.MAX_DAYS - 1)))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("the start date cannot be after the end date")
	void rejectsInvertedRange() {
		assertThatThrownBy(() -> new StatementQuery(CUSTOMER_ID, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("The start date must not be after the end date");
	}

	@Test
	@DisplayName("the range cannot exceed the maximum number of days")
	void rejectsTooLongRange() {
		LocalDate from = LocalDate.of(2025, 1, 1);

		assertThatThrownBy(() -> new StatementQuery(CUSTOMER_ID, from, from.plusDays(StatementQuery.MAX_DAYS)))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("The date range cannot exceed 366 days");
	}
}
