package com.devsu.customers.domain.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import com.devsu.customers.domain.exception.DomainValidationException;

public class Customer extends Person {

	static final int MINIMUM_AGE = 18;

	private final UUID customerId;
	private String password;
	private boolean active;

	private Customer(Long id, UUID customerId, PersonalData personalData, String password, boolean active) {
		super(id, personalData);
		if (customerId == null) {
			throw new DomainValidationException("Customer id is required");
		}
		this.customerId = customerId;
		changePassword(password);
		this.active = active;
	}

	/**
	 * Creates a new, active, not yet persisted customer with a generated customer id.
	 *
	 * @param encodedPassword already encoded password; the domain never handles plain-text passwords.
	 * @param today           reference date used to validate the minimum age.
	 */
	public static Customer create(PersonalData personalData, String encodedPassword, LocalDate today) {
		ensureMinimumAge(personalData, today);
		return new Customer(null, UUID.randomUUID(), personalData, encodedPassword, true);
	}

	/**
	 * Rebuilds an existing customer from its persisted state. Business rules were already applied on creation.
	 */
	public static Customer restore(Long id, UUID customerId, PersonalData personalData, String encodedPassword,
			boolean active) {
		return new Customer(id, customerId, personalData, encodedPassword, active);
	}

	/**
	 * Identification and birth date are identity data verified when the customer was onboarded, so they can never
	 * change. The minimum age therefore only needs to be checked on creation.
	 */
	public void changePersonalData(PersonalData personalData) {
		if (personalData == null) {
			throw new DomainValidationException("Personal data is required");
		}
		PersonalData current = getPersonalData();
		if (!current.identification().equals(personalData.identification())) {
			throw new DomainValidationException("Identification cannot be changed");
		}
		if (!current.birthDate().equals(personalData.birthDate())) {
			throw new DomainValidationException("Birth date cannot be changed");
		}
		replacePersonalData(personalData);
	}

	public void changePassword(String encodedPassword) {
		if (encodedPassword == null || encodedPassword.isBlank()) {
			throw new DomainValidationException("Password is required");
		}
		this.password = encodedPassword;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	public void changeStatus(boolean active) {
		if (active) {
			activate();
		} else {
			deactivate();
		}
	}

	private static void ensureMinimumAge(PersonalData personalData, LocalDate today) {
		if (personalData == null) {
			throw new DomainValidationException("Personal data is required");
		}
		LocalDate birthDate = personalData.birthDate();
		if (birthDate.isAfter(today)) {
			throw new DomainValidationException("Birth date cannot be in the future");
		}
		if (Period.between(birthDate, today).getYears() < MINIMUM_AGE) {
			throw new DomainValidationException("Customer must be at least " + MINIMUM_AGE + " years old");
		}
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public String getPassword() {
		return password;
	}

	public boolean isActive() {
		return active;
	}
}
