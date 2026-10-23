package com.devsu.accounts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.port.in.QueryMovementsUseCase;
import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.InsufficientFundsException;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.domain.model.MovementType;

/**
 * Web layer slice test for POST /api/movimientos.
 */
@WebMvcTest(MovementController.class)
class MovementControllerTest {

	private static final UUID MOVEMENT_ID = UUID.fromString("0b8f0f3e-6a2e-4c3b-9a55-6c4e1d2f7a10");

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RegisterMovementUseCase registerMovement;

	@MockitoBean
	private QueryMovementsUseCase queryMovements;

	@Test
	@DisplayName("a valid withdrawal returns 201 with the resulting balance")
	void registerReturnsMovement() {
		when(registerMovement.register(new RegisterMovementCommand("478758", new BigDecimal("-575"))))
				.thenReturn(new Movement(MOVEMENT_ID, 1L, "478758", MovementType.WITHDRAWAL, new BigDecimal("-575.00"),
						new BigDecimal("1425.00"), Instant.parse("2026-10-22T15:00:00Z")));

		MvcTestResult result = post("{ \"accountNumber\": \"478758\", \"amount\": -575 }");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).headers().hasValue("Location", "http://localhost/api/movimientos/" + MOVEMENT_ID);
		assertThat(result).bodyJson().extractingPath("$.movementType").isEqualTo("WITHDRAWAL");
		assertThat(result).bodyJson().extractingPath("$.amount").isEqualTo(-575.00);
		assertThat(result).bodyJson().extractingPath("$.balance").isEqualTo(1425.00);
	}

	@Test
	@DisplayName("F3: a withdrawal without funds returns 422 with 'Saldo no disponible'")
	void registerWithoutFundsReturnsUnprocessable() {
		when(registerMovement.register(any(RegisterMovementCommand.class))).thenThrow(new InsufficientFundsException());

		MvcTestResult result = post("{ \"accountNumber\": \"496825\", \"amount\": -1 }");

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Insufficient funds");
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Saldo no disponible");
	}

	@Test
	@DisplayName("an unknown account returns 422")
	void registerOnUnknownAccountReturnsUnprocessable() {
		when(registerMovement.register(any(RegisterMovementCommand.class)))
				.thenThrow(AccountNotAvailableException.notFound("999999"));

		MvcTestResult result = post("{ \"accountNumber\": \"999999\", \"amount\": 10 }");

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Account 999999 does not exist");
	}

	@Test
	@DisplayName("missing fields return 400 and never reach the use case")
	void registerRejectsMissingFields() {
		MvcTestResult result = post("{ \"accountNumber\": \"\" }");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.accountNumber").isEqualTo("must not be blank");
		assertThat(result).bodyJson().extractingPath("$.errors.amount").isEqualTo("must not be null");
		verifyNoInteractions(registerMovement);
	}

	private MvcTestResult post(String body) {
		return mvc.post().uri("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}
}
