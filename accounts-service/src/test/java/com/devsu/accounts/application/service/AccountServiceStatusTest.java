package com.devsu.accounts.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsu.accounts.application.port.out.AccountNumberGeneratorPort;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.exception.AccountNotFoundException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.CustomerReference;

@ExtendWith(MockitoExtension.class)
class AccountServiceStatusTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");

	@Mock
	private AccountRepositoryPort accountRepository;

	@Mock
	private CustomerReferenceRepositoryPort customerReferenceRepository;

	@Mock
	private AccountNumberGeneratorPort accountNumberGenerator;

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(accountRepository, customerReferenceRepository, accountNumberGenerator);
	}

	@Test
	@DisplayName("deactivation locks the account and saves it without checking the customer")
	void deactivateSavesAccount() {
		Account account = account(true);
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account));
		when(accountRepository.save(account)).thenReturn(account);

		Account result = accountService.changeStatus("478758", false);

		assertThat(result.isActive()).isFalse();
		verifyNoInteractions(customerReferenceRepository);
	}

	@Test
	@DisplayName("reactivation is rejected while the customer is inactive, without saving")
	void reactivateWithInactiveCustomerFails() {
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account(false)));
		when(customerReferenceRepository.findById(CUSTOMER_ID))
				.thenReturn(Optional.of(new CustomerReference(CUSTOMER_ID, "Jose Lema", false)));

		assertThatThrownBy(() -> accountService.changeStatus("478758", true))
				.isInstanceOf(CustomerNotAvailableException.class);
		verify(accountRepository, never()).save(any());
	}

	@Test
	@DisplayName("changing the status of an unknown account fails")
	void changeStatusOfUnknownAccountFails() {
		when(accountRepository.findByAccountNumberForUpdate("999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.changeStatus("999999", false))
				.isInstanceOf(AccountNotFoundException.class)
				.hasMessage("Account not found with number 999999");
	}

	private static Account account(boolean active) {
		return Account.restore(1L, "478758", AccountType.SAVINGS, new BigDecimal("500"), new BigDecimal("500"), active,
				CUSTOMER_ID);
	}
}
