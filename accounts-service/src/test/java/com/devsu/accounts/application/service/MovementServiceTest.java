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

import com.devsu.accounts.application.port.in.MovementRegistration;
import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.application.port.out.MovementRepositoryPort;
import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.IdempotencyKeyReusedException;
import com.devsu.accounts.domain.exception.InsufficientFundsException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.domain.model.MovementType;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");
	private static final String KEY = "6f1c2a9e-0c43-4b0e-9a6e-3b2f0d8e1c55";

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

		MovementRegistration registration = movementService.register(command("-575", null));

		verify(accountRepository).save(account);
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("1425.00"));
		ArgumentCaptor<Movement> saved = ArgumentCaptor.forClass(Movement.class);
		verify(movementRepository).save(saved.capture());
		assertThat(saved.getValue().balance()).isEqualTo(new BigDecimal("1425.00"));
		assertThat(saved.getValue().occurredAt()).isEqualTo(NOW);
		assertThat(registration.movement()).isSameAs(saved.getValue());
		assertThat(registration.replayed()).isFalse();
	}

	@Test
	@DisplayName("the idempotency key is stored with a new movement")
	void registerStoresIdempotencyKey() {
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account("2000")));
		when(movementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
		when(customerReferenceRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer()));
		when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MovementRegistration registration = movementService.register(command("-575", KEY));

		assertThat(registration.movement().idempotencyKey()).isEqualTo(KEY);
		assertThat(registration.replayed()).isFalse();
	}

	@Test
	@DisplayName("a retry with the same key returns the original movement without moving money again")
	void retryWithSameKeyIsReplayed() {
		Account account = account("1425");
		Movement original = new Movement(UUID.randomUUID(), 1L, "478758", MovementType.WITHDRAWAL,
				new BigDecimal("-575.00"), new BigDecimal("1425.00"), NOW, KEY);
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account));
		when(movementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(original));

		MovementRegistration registration = movementService.register(command("-575", KEY));

		assertThat(registration.replayed()).isTrue();
		assertThat(registration.movement()).isSameAs(original);
		assertThat(account.getAvailableBalance()).isEqualTo(new BigDecimal("1425.00"));
		verify(accountRepository, never()).save(any());
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("reusing a key for a different movement is rejected")
	void reusedKeyForDifferentMovementFails() {
		Movement original = new Movement(UUID.randomUUID(), 1L, "478758", MovementType.WITHDRAWAL,
				new BigDecimal("-575.00"), new BigDecimal("1425.00"), NOW, KEY);
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account("1425")));
		when(movementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(original));

		assertThatThrownBy(() -> movementService.register(command("-600", KEY)))
				.isInstanceOf(IdempotencyKeyReusedException.class)
				.hasMessage("Idempotency-Key " + KEY + " was already used for a different movement");
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("register without funds saves nothing")
	void registerWithoutFundsSavesNothing() {
		when(accountRepository.findByAccountNumberForUpdate("478758")).thenReturn(Optional.of(account("100")));
		when(customerReferenceRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer()));

		assertThatThrownBy(() -> movementService.register(command("-101", null)))
				.isInstanceOf(InsufficientFundsException.class)
				.hasMessage("Saldo no disponible");
		verify(accountRepository, never()).save(any());
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("register on an unknown account fails")
	void registerOnUnknownAccountFails() {
		when(accountRepository.findByAccountNumberForUpdate("999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> movementService.register(new RegisterMovementCommand("999999", BigDecimal.TEN, null)))
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

		assertThatThrownBy(() -> movementService.register(command("10", null)))
				.isInstanceOf(CustomerNotAvailableException.class)
				.hasMessage("Customer " + CUSTOMER_ID + " is inactive");
		verify(movementRepository, never()).save(any());
	}

	private static RegisterMovementCommand command(String amount, String key) {
		return new RegisterMovementCommand("478758", new BigDecimal(amount), key);
	}

	private static Account account(String balance) {
		return Account.restore(1L, "478758", AccountType.SAVINGS, new BigDecimal(balance), new BigDecimal(balance),
				true, CUSTOMER_ID);
	}

	private static CustomerReference activeCustomer() {
		return new CustomerReference(CUSTOMER_ID, "Jose Lema", true);
	}
}
