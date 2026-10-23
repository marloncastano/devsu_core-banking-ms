package com.devsu.accounts.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.application.report.AccountStatement;
import com.devsu.accounts.application.report.StatementQuery;
import com.devsu.accounts.domain.exception.CustomerNotFoundException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.domain.model.MovementType;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final ZoneId BANK_ZONE = ZoneId.of("America/Guayaquil");
	private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
	private static final LocalDate TO = LocalDate.of(2026, 10, 31);

	@Mock
	private CustomerReferenceRepositoryPort customerReferenceRepository;

	@Mock
	private AccountRepositoryPort accountRepository;

	@Mock
	private MovementRepositoryPort movementRepository;

	private ReportService reportService;

	@BeforeEach
	void setUp() {
		reportService = new ReportService(customerReferenceRepository, accountRepository, movementRepository,
				Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), BANK_ZONE));
	}

	@Test
	@DisplayName("the statement lists every account with its movements and totals (use case: Marianela Montalvo)")
	void statementGroupsMovementsByAccount() {
		Account checking = account(1L, "225487", AccountType.CHECKING, "100", "700");
		Account savings = account(2L, "496825", AccountType.SAVINGS, "540", "0");
		Account unused = account(3L, "100000", AccountType.SAVINGS, "50", "50");
		when(customerReferenceRepository.findById(CUSTOMER_ID))
				.thenReturn(Optional.of(new CustomerReference(CUSTOMER_ID, "Marianela Montalvo", true)));
		when(accountRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(checking, savings, unused));
		when(movementRepository.findByCustomerBetween(CUSTOMER_ID, startOfDay(FROM), startOfDay(TO.plusDays(1))))
				.thenReturn(List.of(
						movement(checking, MovementType.DEPOSIT, "600", "700"),
						movement(savings, MovementType.WITHDRAWAL, "-540", "0")));

		AccountStatement statement = reportService.generate(new StatementQuery(CUSTOMER_ID, FROM, TO));

		assertThat(statement.customer().name()).isEqualTo("Marianela Montalvo");
		assertThat(statement.accounts()).hasSize(3);
		AccountStatement.Entry checkingEntry = statement.accounts().get(0);
		assertThat(checkingEntry.account().getAccountNumber()).isEqualTo("225487");
		assertThat(checkingEntry.movements()).hasSize(1);
		assertThat(checkingEntry.totalCredits()).isEqualTo(new BigDecimal("600.00"));
		assertThat(checkingEntry.totalDebits()).isEqualTo(new BigDecimal("0.00"));
		assertThat(statement.accounts().get(1).totalDebits()).isEqualTo(new BigDecimal("540.00"));
		assertThat(statement.accounts().get(2).movements()).isEmpty();
	}

	@Test
	@DisplayName("the date range covers whole days in the bank's time zone")
	void dateRangeUsesBankTimeZone() {
		when(customerReferenceRepository.findById(CUSTOMER_ID))
				.thenReturn(Optional.of(new CustomerReference(CUSTOMER_ID, "Jose Lema", true)));
		when(accountRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

		reportService.generate(new StatementQuery(CUSTOMER_ID, FROM, TO));

		// Guayaquil is UTC-5: 1 Oct 00:00 local = 05:00 UTC; the end is exclusive at 1 Nov 00:00 local.
		verify(movementRepository).findByCustomerBetween(CUSTOMER_ID, Instant.parse("2026-10-01T05:00:00Z"),
				Instant.parse("2026-11-01T05:00:00Z"));
	}

	@Test
	@DisplayName("an unknown customer fails")
	void unknownCustomerFails() {
		when(customerReferenceRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reportService.generate(new StatementQuery(CUSTOMER_ID, FROM, TO)))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found with id " + CUSTOMER_ID);
	}

	private static Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(BANK_ZONE).toInstant();
	}

	private static Account account(Long id, String number, AccountType type, String initial, String available) {
		return Account.restore(id, number, type, new BigDecimal(initial), new BigDecimal(available), true, CUSTOMER_ID);
	}

	private static Movement movement(Account account, MovementType type, String amount, String balance) {
		return new Movement(UUID.randomUUID(), account.getId(), account.getAccountNumber(), type,
				new BigDecimal(amount).setScale(2), new BigDecimal(balance).setScale(2),
				Instant.parse("2026-10-10T15:00:00Z"), null);
	}
}
