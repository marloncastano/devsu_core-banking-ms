package com.devsu.customers.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devsu.customers.application.port.out.CustomerRepositoryPort;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.domain.model.PersonalData;

@Component
class CustomerPersistenceAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository customerJpaRepository;

	CustomerPersistenceAdapter(CustomerJpaRepository customerJpaRepository) {
		this.customerJpaRepository = customerJpaRepository;
	}

	/**
	 * Updates load the managed entity and copy only business fields, so audit columns are never overwritten.
	 */
	@Override
	public Customer save(Customer customer) {
		CustomerEntity entity = customer.getId() == null
				? newEntity(customer)
				: customerJpaRepository.findById(customer.getId())
						.orElseThrow(() -> new IllegalStateException("Customer to update does not exist: "
								+ customer.getCustomerId()));
		copyBusinessFields(customer, entity);
		return toDomain(customerJpaRepository.save(entity));
	}

	@Override
	public Optional<Customer> findByCustomerId(UUID customerId) {
		return customerJpaRepository.findByCustomerId(customerId).map(CustomerPersistenceAdapter::toDomain);
	}

	@Override
	public List<Customer> findAll() {
		return customerJpaRepository.findAll().stream().map(CustomerPersistenceAdapter::toDomain).toList();
	}

	@Override
	public boolean existsByIdentification(String identification) {
		return customerJpaRepository.existsByIdentification(identification);
	}

	private static CustomerEntity newEntity(Customer customer) {
		CustomerEntity entity = new CustomerEntity();
		entity.setCustomerId(customer.getCustomerId());
		return entity;
	}

	private static void copyBusinessFields(Customer customer, CustomerEntity entity) {
		PersonalData data = customer.getPersonalData();
		entity.setName(data.name());
		entity.setGender(data.gender());
		entity.setBirthDate(data.birthDate());
		entity.setIdentification(data.identification());
		entity.setAddress(data.address());
		entity.setPhone(data.phone());
		entity.setPassword(customer.getPassword());
		entity.setActive(customer.isActive());
	}

	private static Customer toDomain(CustomerEntity entity) {
		PersonalData data = new PersonalData(entity.getName(), entity.getGender(), entity.getBirthDate(),
				entity.getIdentification(), entity.getAddress(), entity.getPhone());
		return Customer.restore(entity.getId(), entity.getCustomerId(), data, entity.getPassword(), entity.isActive());
	}
}
