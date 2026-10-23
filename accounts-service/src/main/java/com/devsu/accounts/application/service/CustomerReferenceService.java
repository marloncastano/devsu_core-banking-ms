package com.devsu.accounts.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.accounts.application.port.in.SyncCustomerCommand;
import com.devsu.accounts.application.port.in.SyncCustomerUseCase;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.model.CustomerReference;

@Service
@Transactional
public class CustomerReferenceService implements SyncCustomerUseCase {

	private static final Logger log = LoggerFactory.getLogger(CustomerReferenceService.class);

	private final CustomerReferenceRepositoryPort customerReferenceRepository;

	public CustomerReferenceService(CustomerReferenceRepositoryPort customerReferenceRepository) {
		this.customerReferenceRepository = customerReferenceRepository;
	}

	@Override
	public void sync(SyncCustomerCommand command) {
		CustomerReference customer = new CustomerReference(command.customerId(), command.name(), command.active());
		if (!customerReferenceRepository.saveIfNotOlder(customer, command.occurredAt())) {
			log.info("Ignored outdated change for customer {} from {}", command.customerId(), command.occurredAt());
		}
	}
}
