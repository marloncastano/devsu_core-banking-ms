package com.devsu.accounts.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.devsu.accounts.domain.model.CustomerReference;

public interface CustomerReferenceRepositoryPort {

	Optional<CustomerReference> findById(UUID customerId);
}
