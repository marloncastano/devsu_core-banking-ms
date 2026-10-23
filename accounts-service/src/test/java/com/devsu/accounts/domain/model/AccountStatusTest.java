package com.devsu.accounts.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.DomainValidationException;

class AccountStatusTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");

	@Test
	@DisplayName("deactivating freezes the account but keeps its balance")
	void deactivateKeepsBalanceAndBlocksMovements() {
		Account account = account(true);

		account.deactivate();

		assertThat(account.isActive()).isFalse();
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("500.00"));
		assertThatThrownBy(() -> account.registerMovement(BigDecimal.ONE, Instant.now()))
				.isInstanceOf(AccountNotAvailableException.class);
	}

	@Test
	@DisplayName("an account can be reactivated while its owner is active")
	void activateWithActiveOwner() {
		Account account = account(false);

		account.activate(new CustomerReference(CUSTOMER_ID, "Jose Lema", true));

		assertThat(account.isActive()).isTrue();
	}

	@Test
	@DisplayName("an account cannot be reactivated while its owner is inactive")
	void activateWithInactiveOwnerFails() {
		Account account = account(false);

		assertThatThrownBy(() -> account.activate(new CustomerReference(CUSTOMER_ID, "Jose Lema", false)))
				.isInstanceOf(CustomerNotAvailableException.class)
				.hasMessage("Customer " + CUSTOMER_ID + " is inactive");
		assertThat(account.isActive()).isFalse();
	}

	@Test
	@DisplayName("activation requires the actual owner of the account")
	void activateWithAnotherCustomerFails() {
		Account account = account(false);
		CustomerReference stranger = new CustomerReference(UUID.randomUUID(), "Someone else", true);

		assertThatThrownBy(() -> account.activate(stranger)).isInstanceOf(DomainValidationException.class);
		assertThat(account.isActive()).isFalse();
	}

	private static Account account(boolean active) {
		return Account.restore(1L, "478758", AccountType.SAVINGS, new BigDecimal("500"), new BigDecimal("500"), active,
				CUSTOMER_ID);
	}
}
