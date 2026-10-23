package com.devsu.accounts.application.port.out;

import com.devsu.accounts.domain.model.Movement;

public interface MovementRepositoryPort {

	Movement save(Movement movement);
}
