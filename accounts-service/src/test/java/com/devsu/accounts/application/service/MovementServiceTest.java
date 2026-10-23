package com.devsu.accounts.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.InsufficientFundsException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");

	@Mock
	private AccountRepositoryPort accountRepository;

	@Mock
	private CustomerReferenceRepositoryPort customerReferenceRepository;

	@Mock
	private MovementRepositoryPort movementRepository;

	private MovementService movementService;

	@BeforeEach
	void setUp() {
		movementService = new MovementService(accountRepository, customerReferenceRepository, movementRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("register locks the account, saves the new balance and records the movement")
	void registerSavesBalanceAndMovement() {
		Account account = account("2000");
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account));
		when(customerReferenceRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer()));
		when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Movement movement = movementService.register(new RegisterMovementCommand("478758", new BigDecimal("-575")));

		verify(accountRepository).save(account);
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("1425.00"));
		ArgumentCaptor<Movement> saved = ArgumentCaptor.forClass(Movement.class);
		verify(movementRepository).save(saved.capture());
		assertThat(saved.getValue().balance()).isEqualTo(new BigDecimal("1425.00"));
		assertThat(saved.getValue().occurredAt()).isEqualTo(NOW);
		assertThat(movement).isSameAs(saved.getValue());
	}

	@Test
	@DisplayName("register without funds saves nothing")
	void registerWithoutFundsSavesNothing() {
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account("100")));
		when(customerReferenceRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer()));

		assertThatThrownBy(() -> movementService.register(new RegisterMovementCommand("478758", new BigDecimal("-101"))))
				.isInstanceOf(InsufficientFundsException.class)
				.hasMessage("Saldo no disponible");
		verify(accountRepository, never()).save(any());
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("register on an unknown account fails")
	void registerOnUnknownAccountFails() {
		when(accountRepository.findByAccountNumberForUpdate("999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> movementService.register(new RegisterMovementCommand("999999", BigDecimal.TEN)))
				.isInstanceOf(AccountNotAvailableException.class)
				.hasMessage("Account 999999 does not exist");
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("a deactivated customer cannot move money")
	void registerForInactiveCustomerFails() {
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account("100")));
		when(customerReferenceRepository.findById(CUSTOMER_ID))
				.thenReturn(Optional.of(new CustomerReference(CUSTOMER_ID, "Jose Lema", false)));

		assertThatThrownBy(() -> movementService.register(new RegisterMovementCommand("478758", BigDecimal.TEN)))
				.isInstanceOf(CustomerNotAvailableException.class)
				.hasMessage("Customer " + CUSTOMER_ID + " is inactive");
		verify(movementRepository, never()).save(any());
	}

	private static Account account(String balance) {
		return Account.restore(1L, "478758", AccountType.SAVINGS, new BigDecimal(balance), new BigDecimal(balance),
				true, CUSTOMER_ID);
	}

	private static CustomerReference activeCustomer() {
		return new CustomerReference(CUSTOMER_ID, "Jose Lema", true);
	}
}
