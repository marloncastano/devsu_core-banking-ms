package com.devsu.customers.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

	Optional<CustomerEntity> findByCustomerId(UUID customerId);

	boolean existsByIdentification(String identification);
}
