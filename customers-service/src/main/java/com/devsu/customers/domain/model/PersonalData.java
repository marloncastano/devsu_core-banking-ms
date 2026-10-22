package com.devsu.customers.domain.model;

import java.time.LocalDate;

import com.devsu.customers.domain.exception.DomainValidationException;

/**
 * Personal data of a person. Validated on construction, so an instance is always consistent.
 */
public record PersonalData(
		String name,
		Gender gender,
		LocalDate birthDate,
		String identification,
		String address,
		String phone) {

	public PersonalData {
		requireText(name, "name");
		requireText(identification, "identification");
		requireText(address, "address");
		requireText(phone, "phone");
		if (gender == null) {
			throw new DomainValidationException("Gender is required");
		}
		if (birthDate == null) {
			throw new DomainValidationException("Birth date is required");
		}
		name = name.strip();
		identification = identification.strip();
		address = address.strip();
		phone = phone.strip();
	}

	private static void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new DomainValidationException("Field " + field + " is required");
		}
	}
}
