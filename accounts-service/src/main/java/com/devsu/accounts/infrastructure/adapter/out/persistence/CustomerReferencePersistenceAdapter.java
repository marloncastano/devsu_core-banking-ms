package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.model.CustomerReference;

@Component
class CustomerReferencePersistenceAdapter implements CustomerReferenceRepositoryPort {

	private final CustomerReferenceJpaRepository customerReferenceJpaRepository;

	CustomerReferencePersistenceAdapter(CustomerReferenceJpaRepository customerReferenceJpaRepository) {
		this.customerReferenceJpaRepository = customerReferenceJpaRepository;
	}

	@Override
	public Optional<CustomerReference> findById(UUID customerId) {
		return customerReferenceJpaRepository.findById(customerId)
				.map(entity -> new CustomerReference(entity.getCustomerId(), entity.getName(), entity.isActive()));
	}
}
