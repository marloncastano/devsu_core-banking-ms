package com.devsu.accounts.application.port.in;

import com.devsu.accounts.domain.model.Movement;

public interface RegisterMovementUseCase {

	Movement register(RegisterMovementCommand command);
}
