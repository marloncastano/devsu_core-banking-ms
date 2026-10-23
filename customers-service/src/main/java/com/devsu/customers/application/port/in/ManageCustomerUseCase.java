package com.devsu.customers.application.port.in;

import java.util.UUID;

import com.devsu.customers.application.pagination.PageQuery;
import com.devsu.customers.application.pagination.PageResult;
import com.devsu.customers.domain.model.Customer;

public interface ManageCustomerUseCase {

	Customer create(CreateCustomerCommand command);

	Customer getByCustomerId(UUID customerId);

	PageResult<Customer> list(PageQuery query);

	Customer patch(UUID customerId, PatchCustomerCommand command);

	/**
	 * Soft delete: the customer is deactivated and its history is kept.
	 */
	void delete(UUID customerId);
}
