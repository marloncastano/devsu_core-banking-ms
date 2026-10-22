package com.devsu.customers.application.port.in;

import java.util.List;
import java.util.UUID;

import com.devsu.customers.domain.model.Customer;

public interface ManageCustomerUseCase {

	Customer create(CreateCustomerCommand command);

	Customer getByCustomerId(UUID customerId);

	List<Customer> list();

	Customer update(UUID customerId, UpdateCustomerCommand command);

	Customer patch(UUID customerId, PatchCustomerCommand command);

	/**
	 * Soft delete: the customer is deactivated and its history is kept.
	 */
	void delete(UUID customerId);
}
