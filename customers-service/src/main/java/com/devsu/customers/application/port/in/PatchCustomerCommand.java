package com.devsu.customers.application.port.in;

import java.time.LocalDate;

import com.devsu.customers.domain.model.Gender;
import com.devsu.customers.domain.model.PersonalData;

/**
 * Partial update of a customer (PATCH): null fields keep their current value.
 */
public record PatchCustomerCommand(
		String name,
		Gender gender,
		LocalDate birthDate,
		String identification,
		String address,
		String phone,
		String password,
		Boolean active) {

	public PersonalData applyTo(PersonalData current) {
		return new PersonalData(
				valueOrCurrent(name, current.name()),
				valueOrCurrent(gender, current.gender()),
				valueOrCurrent(birthDate, current.birthDate()),
				valueOrCurrent(identification, current.identification()),
				valueOrCurrent(address, current.address()),
				valueOrCurrent(phone, current.phone()));
	}

	private static <T> T valueOrCurrent(T value, T current) {
		return value != null ? value : current;
	}
}
