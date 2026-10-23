package com.devsu.customers.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsu.customers.application.pagination.PageQuery;
import com.devsu.customers.application.pagination.PageResult;
import com.devsu.customers.application.port.in.CreateCustomerCommand;
import com.devsu.customers.application.port.in.ManageCustomerUseCase;
import com.devsu.customers.application.port.in.PatchCustomerCommand;
import com.devsu.customers.application.port.out.CustomerRepositoryPort;
import com.devsu.customers.application.port.out.PasswordEncoderPort;
import com.devsu.customers.domain.exception.CustomerNotFoundException;
import com.devsu.customers.domain.exception.DuplicateCustomerException;
import com.devsu.customers.domain.model.Customer;

@Service
@Transactional
public class CustomerService implements ManageCustomerUseCase {

	private final CustomerRepositoryPort customerRepository;
	private final PasswordEncoderPort passwordEncoder;
	private final Clock clock;

	public CustomerService(CustomerRepositoryPort customerRepository, PasswordEncoderPort passwordEncoder,
			Clock clock) {
		this.customerRepository = customerRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	public Customer create(CreateCustomerCommand command) {
		ensureIdentificationAvailable(command.personalData().identification());

		Customer customer = Customer.create(command.personalData(), passwordEncoder.encode(command.password()), today());
		return customerRepository.save(customer);
	}

	@Override
	@Transactional(readOnly = true)
	public Customer getByCustomerId(UUID customerId) {
		return findCustomer(customerId);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Customer> list(PageQuery query) {
		return customerRepository.findAll(query);
	}

	@Override
	public Customer patch(UUID customerId, PatchCustomerCommand command) {
		Customer customer = findCustomer(customerId);
		customer.changePersonalData(command.applyTo(customer.getPersonalData()));
		if (command.password() != null) {
			customer.changePassword(passwordEncoder.encode(command.password()));
		}
		if (command.active() != null) {
			customer.changeStatus(command.active());
		}
		return customerRepository.save(customer);
	}

	@Override
	public void delete(UUID customerId) {
		Customer customer = findCustomer(customerId);
		customer.deactivate();
		customerRepository.save(customer);
	}

	private Customer findCustomer(UUID customerId) {
		return customerRepository.findByCustomerId(customerId)
				.orElseThrow(() -> new CustomerNotFoundException(customerId));
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private void ensureIdentificationAvailable(String identification) {
		if (customerRepository.existsByIdentification(identification)) {
			throw DuplicateCustomerException.byIdentification(identification);
		}
	}
}
