package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.port.in.MovementRegistration;
import com.devsu.accounts.application.port.in.QueryMovementsUseCase;
import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.MovementResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.PageResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.RegisterMovementRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * The resource path is kept in Spanish because it is part of the required API contract.
 */
@RestController
@RequestMapping("/api/movimientos")
public class MovementController {

	static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	static final String REPLAYED_HEADER = "Idempotent-Replayed";

	private final RegisterMovementUseCase registerMovement;
	private final QueryMovementsUseCase queryMovements;

	public MovementController(RegisterMovementUseCase registerMovement, QueryMovementsUseCase queryMovements) {
		this.registerMovement = registerMovement;
		this.queryMovements = queryMovements;
	}

	/**
	 * The optional Idempotency-Key header makes retries safe: repeating a request with the same key returns the
	 * original movement instead of moving money twice.
	 */
	@PostMapping
	public ResponseEntity<MovementResponse> register(
			@RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) @Size(min = 1, max = 100) String key,
			@Valid @RequestBody RegisterMovementRequest request) {
		MovementRegistration registration = registerMovement.register(
				new RegisterMovementCommand(request.accountNumber(), request.amount(), key));
		Movement movement = registration.movement();
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{movementId}")
				.buildAndExpand(movement.movementId())
				.toUri();
		return ResponseEntity.created(location)
				.header(REPLAYED_HEADER, String.valueOf(registration.replayed()))
				.body(toResponse(movement));
	}

	@GetMapping
	public PageResponse<MovementResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size,
			@RequestParam(required = false) String accountNumber) {
		return PageResponse.from(queryMovements.list(new PageQuery(page, size), accountNumber)
				.map(MovementController::toResponse));
	}

	@GetMapping("/{movementId}")
	public MovementResponse getByMovementId(@PathVariable UUID movementId) {
		return toResponse(queryMovements.getByMovementId(movementId));
	}

	private static MovementResponse toResponse(Movement movement) {
		return new MovementResponse(movement.movementId(), movement.accountNumber(), movement.type(),
				movement.amount(), movement.balance(), movement.occurredAt());
	}
}
