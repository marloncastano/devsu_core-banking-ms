package com.devsu.accounts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.application.port.in.QueryMovementsUseCase;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.domain.exception.MovementNotFoundException;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.domain.model.MovementType;

/**
 * Web layer slice test for GET /api/movimientos and GET /api/movimientos/{movementId}.
 */
@WebMvcTest(MovementController.class)
class MovementControllerQueryTest {

	private static final UUID MOVEMENT_ID = UUID.fromString("0b8f0f3e-6a2e-4c3b-9a55-6c4e1d2f7a10");
	private static final Movement MOVEMENT = new Movement(MOVEMENT_ID, 1L, "478758", MovementType.WITHDRAWAL,
			new BigDecimal("-575.00"), new BigDecimal("1425.00"), Instant.parse("2026-10-22T15:00:00Z"));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RegisterMovementUseCase registerMovement;

	@MockitoBean
	private QueryMovementsUseCase queryMovements;

	@Test
	@DisplayName("get returns the movement with the resulting balance")
	void getReturnsMovement() {
		when(queryMovements.getByMovementId(MOVEMENT_ID)).thenReturn(MOVEMENT);

		MvcTestResult result = mvc.get().uri("/api/movimientos/" + MOVEMENT_ID).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.accountNumber").isEqualTo("478758");
		assertThat(result).bodyJson().extractingPath("$.balance").isEqualTo(1425.00);
	}

	@Test
	@DisplayName("get of an unknown movement returns 404")
	void getReturnsNotFound() {
		when(queryMovements.getByMovementId(MOVEMENT_ID)).thenThrow(new MovementNotFoundException(MOVEMENT_ID));

		MvcTestResult result = mvc.get().uri("/api/movimientos/" + MOVEMENT_ID).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Movement not found with id " + MOVEMENT_ID);
	}

	@Test
	@DisplayName("get with an invalid id returns 400")
	void getRejectsInvalidId() {
		MvcTestResult result = mvc.get().uri("/api/movimientos/abc").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(queryMovements);
	}

	@Test
	@DisplayName("list can be filtered by account")
	void listFiltersByAccount() {
		when(queryMovements.list(new PageQuery(0, 20), "478758"))
				.thenReturn(new PageResult<>(List.of(MOVEMENT), 0, 20, 1));

		MvcTestResult result = mvc.get().uri("/api/movimientos?accountNumber=478758").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.content[0].movementId").isEqualTo(MOVEMENT_ID.toString());
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
	}

	@Test
	@DisplayName("size above the maximum returns 400")
	void listRejectsSizeAboveMaximum() {
		MvcTestResult result = mvc.get().uri("/api/movimientos?size=500").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(queryMovements);
	}
}
