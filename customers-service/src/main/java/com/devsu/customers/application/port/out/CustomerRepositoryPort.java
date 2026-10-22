package com.devsu.customers.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devsu.customers.domain.model.Customer;

public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findByCustomerId(UUID customerId);

	List<Customer> findAll();

	boolean existsByIdentification(String identification);
}
