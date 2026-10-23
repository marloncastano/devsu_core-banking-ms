package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.domain.model.Movement;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.MovementResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.RegisterMovementRequest;

import jakarta.validation.Valid;

/**
 * The resource path is kept in Spanish because it is part of the required API contract.
 */
@RestController
@RequestMapping("/api/movimientos")
public class MovementController {

	private final RegisterMovementUseCase registerMovement;

	public MovementController(RegisterMovementUseCase registerMovement) {
		this.registerMovement = registerMovement;
	}

	@PostMapping
	public ResponseEntity<MovementResponse> register(@Valid @RequestBody RegisterMovementRequest request) {
		Movement movement = registerMovement.register(
				new RegisterMovementCommand(request.accountNumber(), request.amount()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{movementId}")
				.buildAndExpand(movement.movementId())
				.toUri();
		return ResponseEntity.created(location).body(toResponse(movement));
	}

	private static MovementResponse toResponse(Movement movement) {
		return new MovementResponse(movement.movementId(), movement.accountNumber(), movement.type(),
				movement.amount(), movement.balance(), movement.occurredAt());
	}
}
