package com.devsu.customers.application.port.in;

import com.devsu.customers.domain.model.PersonalData;

public record CreateCustomerCommand(
		PersonalData personalData,
		String password) {
}
