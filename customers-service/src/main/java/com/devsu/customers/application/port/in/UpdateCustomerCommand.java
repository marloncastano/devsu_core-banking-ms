package com.devsu.customers.application.port.in;

import com.devsu.customers.domain.model.PersonalData;

/**
 * Full replacement of a customer's data (PUT).
 */
public record UpdateCustomerCommand(
		PersonalData personalData,
		String password,
		boolean active) {
}
