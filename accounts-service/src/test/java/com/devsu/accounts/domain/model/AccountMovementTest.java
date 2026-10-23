package com.devsu.accounts.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.DomainValidationException;
import com.devsu.accounts.domain.exception.InsufficientFundsException;

/**
 * F2 and F3: movements update the available balance and a balance can never become negative.
 */
class AccountMovementTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");

	@Test
	@DisplayName("a withdrawal decreases the balance (use case: 478758 savings 2000, withdrawal of 575)")
	void withdrawalDecreasesBalance() {
		Account account = account("478758", "2000", true);

		Movement movement = account.registerMovement(new BigDecimal("-575"), NOW);

		assertThat(movement.type()).isEqualTo(MovementType.WITHDRAWAL);
		assertThat(movement.amount()).isEqualTo(new BigDecimal("-575.00"));
		assertThat(movement.balance()).isEqualTo(new BigDecimal("1425.00"));
		assertThat(movement.occurredAt()).isEqualTo(NOW);
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("1425.00"));
		assertThat(account.getInitialBalance()).isEqualTo(new BigDecimal("2000.00"));
	}

	@Test
	@DisplayName("a deposit increases the balance (use case: 225487 checking 100, deposit of 600)")
	void depositIncreasesBalance() {
		Account account = account("225487", "100", true);

		Movement movement = account.registerMovement(new BigDecimal("600"), NOW);

		assertThat(movement.type()).isEqualTo(MovementType.DEPOSIT);
		assertThat(movement.balance()).isEqualTo(new BigDecimal("700.00"));
	}

	@Test
	@DisplayName("withdrawing the whole balance leaves it at zero (use case: 496825 savings 540, withdrawal of 540)")
	void withdrawingWholeBalanceIsAllowed() {
		Account account = account("496825", "540", true);

		Movement movement = account.registerMovement(new BigDecimal("-540"), NOW);

		assertThat(movement.balance()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	@DisplayName("withdrawing more than the balance fails with 'Saldo no disponible' and keeps the balance")
	void withdrawalWithoutFundsFails() {
		Account account = account("496825", "0", true);

		assertThatThrownBy(() -> account.registerMovement(new BigDecimal("-0.01"), NOW))
				.isInstanceOf(InsufficientFundsException.class)
				.hasMessage("Saldo no disponible");
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	@DisplayName("a zero amount is rejected")
	void zeroAmountIsRejected() {
		assertThatThrownBy(() -> account("478758", "100", true).registerMovement(BigDecimal.ZERO, NOW))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Amount must be different from zero");
	}

	@Test
	@DisplayName("an amount with more than two decimals is rejected")
	void tooManyDecimalsAreRejected() {
		assertThatThrownBy(() -> account("478758", "100", true).registerMovement(new BigDecimal("1.005"), NOW))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Amount cannot have more than 2 decimals");
	}

	@Test
	@DisplayName("an inactive account cannot register movements")
	void inactiveAccountIsRejected() {
		assertThatThrownBy(() -> account("478758", "100", false).registerMovement(BigDecimal.TEN, NOW))
				.isInstanceOf(AccountNotAvailableException.class)
				.hasMessage("Account 478758 is inactive");
	}

	private static Account account(String number, String balance, boolean active) {
		return Account.restore(1L, number, AccountType.SAVINGS, new BigDecimal(balance), new BigDecimal(balance), active,
				CUSTOMER_ID);
	}
}
