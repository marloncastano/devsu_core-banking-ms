package com.devsu.customers.infrastructure.adapter.in.rest;

import java.time.LocalDate;

import com.devsu.customers.application.port.in.CreateCustomerCommand;
import com.devsu.customers.application.port.in.PatchCustomerCommand;
import com.devsu.customers.application.port.in.UpdateCustomerCommand;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.domain.model.PersonalData;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.CustomerResponse;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.PatchCustomerRequest;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.UpdateCustomerRequest;

final class CustomerRestMapper {

	private CustomerRestMapper() {
	}

	static CreateCustomerCommand toCommand(CreateCustomerRequest request) {
		PersonalData data = new PersonalData(request.name(), request.gender(), request.birthDate(),
				request.identification(), request.address(), request.phone());
		return new CreateCustomerCommand(data, request.password());
	}

	static UpdateCustomerCommand toCommand(UpdateCustomerRequest request) {
		PersonalData data = new PersonalData(request.name(), request.gender(), request.birthDate(),
				request.identification(), request.address(), request.phone());
		return new UpdateCustomerCommand(data, request.password(), request.active());
	}

	static PatchCustomerCommand toCommand(PatchCustomerRequest request) {
		return new PatchCustomerCommand(request.name(), request.gender(), request.birthDate(), request.identification(),
				request.address(), request.phone(), request.password(), request.active());
	}

	static CustomerResponse toResponse(Customer customer, LocalDate today) {
		PersonalData data = customer.getPersonalData();
		return new CustomerResponse(customer.getCustomerId(), data.name(), data.gender(), data.birthDate(),
				customer.ageOn(today), data.identification(), data.address(), data.phone(), customer.isActive());
	}
}
