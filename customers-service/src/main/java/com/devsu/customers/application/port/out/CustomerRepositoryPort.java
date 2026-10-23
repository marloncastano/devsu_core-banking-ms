package com.devsu.customers.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.devsu.customers.application.pagination.PageQuery;
import com.devsu.customers.application.pagination.PageResult;
import com.devsu.customers.domain.model.Customer;

public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findByCustomerId(UUID customerId);

	PageResult<Customer> findAll(PageQuery query);

	boolean existsByIdentification(String identification);
}
