package com.devsu.accounts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.port.in.GenerateStatementUseCase;
import com.devsu.accounts.application.report.AccountStatement;
import com.devsu.accounts.application.report.StatementQuery;
import com.devsu.accounts.domain.exception.CustomerNotFoundException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.domain.model.MovementType;

/**
 * Web layer slice test for GET /api/reportes.
 */
@WebMvcTest(ReportController.class)
@Import(ReportControllerTest.BankClockConfig.class)
class ReportControllerTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final String URL = "/api/reportes?fecha=2026-10-01,2026-10-31&cliente=" + CUSTOMER_ID;

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GenerateStatementUseCase generateStatement;

	@Test
	@DisplayName("returns the statement grouped by account with balances and movement detail")
	void returnsStatement() {
		Account account = Account.restore(1L, "225487", AccountType.CHECKING, new BigDecimal("100"),
				new BigDecimal("700"), true, CUSTOMER_ID);
		// 03:00 UTC on 11 Oct is still 10 Oct in the bank's time zone (UTC-5).
		Movement deposit = new Movement(UUID.randomUUID(), 1L, "225487", MovementType.DEPOSIT, new BigDecimal("600.00"),
				new BigDecimal("700.00"), Instant.parse("2026-10-11T03:00:00Z"), null);
		when(generateStatement.generate(new StatementQuery(CUSTOMER_ID, LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31)))).thenReturn(new AccountStatement(
						new CustomerReference(CUSTOMER_ID, "Marianela Montalvo", true), LocalDate.of(2026, 10, 1),
						LocalDate.of(2026, 10, 31), List.of(new AccountStatement.Entry(account, List.of(deposit)))));

		MvcTestResult result = mvc.get().uri(URL).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.customerName").isEqualTo("Marianela Montalvo");
		assertThat(result).bodyJson().extractingPath("$.accounts[0].accountNumber").isEqualTo("225487");
		assertThat(result).bodyJson().extractingPath("$.accounts[0].initialBalance").isEqualTo(100.00);
		assertThat(result).bodyJson().extractingPath("$.accounts[0].availableBalance").isEqualTo(700.00);
		assertThat(result).bodyJson().extractingPath("$.accounts[0].totalCredits").isEqualTo(600.00);
		assertThat(result).bodyJson().extractingPath("$.accounts[0].movements[0].date").isEqualTo("2026-10-10");
		assertThat(result).bodyJson().extractingPath("$.accounts[0].movements[0].balance").isEqualTo(700.00);
	}

	@Test
	@DisplayName("a date range without the expected format returns 400")
	void rejectsMalformedDateRange() {
		MvcTestResult result = mvc.get().uri("/api/reportes?fecha=10/02/2022&cliente=" + CUSTOMER_ID).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.fecha")
				.isEqualTo("must have the format yyyy-MM-dd,yyyy-MM-dd");
		verifyNoInteractions(generateStatement);
	}

	@Test
	@DisplayName("an inverted date range returns 400")
	void rejectsInvertedDateRange() {
		MvcTestResult result = mvc.get().uri("/api/reportes?fecha=2026-10-31,2026-10-01&cliente=" + CUSTOMER_ID)
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("The start date must not be after the end date");
	}

	@Test
	@DisplayName("missing parameters return 400")
	void rejectsMissingParameters() {
		MvcTestResult result = mvc.get().uri("/api/reportes?fecha=2026-10-01,2026-10-31").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(generateStatement);
	}

	@Test
	@DisplayName("an unknown customer returns 404")
	void returnsNotFoundForUnknownCustomer() {
		when(generateStatement.generate(new StatementQuery(CUSTOMER_ID, LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31)))).thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

		MvcTestResult result = mvc.get().uri(URL).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
	}

	@TestConfiguration
	static class BankClockConfig {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneId.of("America/Guayaquil"));
		}
	}
}
