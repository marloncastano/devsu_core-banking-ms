package com.devsu.accounts.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.DomainValidationException;

class AccountTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final CustomerReference ACTIVE_CUSTOMER = new CustomerReference(CUSTOMER_ID, "Jose Lema", true);

	@Test
	@DisplayName("open creates an active account whose available balance equals the initial balance")
	void openStartsActiveWithInitialBalance() {
		Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000"), ACTIVE_CUSTOMER);

		assertThat(account.getId()).isNull();
		assertThat(account.isActive()).isTrue();
		assertThat(account.getCustomerId()).isEqualTo(CUSTOMER_ID);
		assertThat(account.getInitialBalance()).isEqualTo(new BigDecimal("2000.00"));
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("2000.00"));
	}

	@Test
	@DisplayName("open accepts a zero initial balance")
	void openAcceptsZeroBalance() {
		Account account = Account.open("495878", AccountType.SAVINGS, BigDecimal.ZERO, ACTIVE_CUSTOMER);

		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	@DisplayName("open rejects an inactive customer")
	void openRejectsInactiveCustomer() {
		CustomerReference inactive = new CustomerReference(CUSTOMER_ID, "Jose Lema", false);

		assertThatThrownBy(() -> Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, inactive))
				.isInstanceOf(CustomerNotAvailableException.class)
				.hasMessage("Customer " + CUSTOMER_ID + " is inactive");
	}

	@Test
	@DisplayName("open rejects a negative initial balance")
	void openRejectsNegativeBalance() {
		assertThatThrownBy(() -> Account.open("478758", AccountType.SAVINGS, new BigDecimal("-1"), ACTIVE_CUSTOMER))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Initial balance cannot be negative");
	}

	@Test
	@DisplayName("open rejects amounts with more than two decimals instead of rounding them")
	void openRejectsMoreThanTwoDecimals() {
		assertThatThrownBy(() -> Account.open("478758", AccountType.SAVINGS, new BigDecimal("10.005"), ACTIVE_CUSTOMER))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Initial balance cannot have more than 2 decimals");
	}

	@Test
	@DisplayName("open rejects a missing account type")
	void openRejectsMissingType() {
		assertThatThrownBy(() -> Account.open("478758", null, BigDecimal.TEN, ACTIVE_CUSTOMER))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Account type is required");
	}
}
