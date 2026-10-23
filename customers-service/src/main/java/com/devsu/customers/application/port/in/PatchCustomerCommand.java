package com.devsu.customers.application.port.in;

import com.devsu.customers.domain.model.Gender;
import com.devsu.customers.domain.model.PersonalData;

/**
 * Partial update of a customer (PATCH): null fields keep their current value. Identification and birth date are immutable.
 */
public record PatchCustomerCommand(
		String name,
		Gender gender,
		String address,
		String phone,
		String password,
		Boolean active) {

	public PersonalData applyTo(PersonalData current) {
		return new PersonalData(
				valueOrCurrent(name, current.name()),
				valueOrCurrent(gender, current.gender()),
				current.birthDate(),
				current.identification(),
				valueOrCurrent(address, current.address()),
				valueOrCurrent(phone, current.phone()));
	}

	private static <T> T valueOrCurrent(T value, T current) {
		return value != null ? value : current;
	}
}
