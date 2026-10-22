package com.devsu.customers.domain.model;

import java.time.LocalDate;
import java.time.Period;

import com.devsu.customers.domain.exception.DomainValidationException;

public abstract class Person {

	private final Long id;
	private PersonalData personalData;

	protected Person(Long id, PersonalData personalData) {
		this.id = id;
		replacePersonalData(personalData);
	}

	/**
	 * Age is derived from the birth date so it never gets outdated.
	 */
	public int ageOn(LocalDate date) {
		return Period.between(personalData.birthDate(), date).getYears();
	}

	protected final void replacePersonalData(PersonalData personalData) {
		if (personalData == null) {
			throw new DomainValidationException("Personal data is required");
		}
		this.personalData = personalData;
	}

	public Long getId() {
		return id;
	}

	public PersonalData getPersonalData() {
		return personalData;
	}
}
