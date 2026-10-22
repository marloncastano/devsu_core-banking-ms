package com.devsu.customers.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsu.customers.domain.exception.DomainValidationException;

class CustomerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
	private static final String ENCODED_PASSWORD = "$2a$10$encoded";

	@Test
	@DisplayName("create generates a customer id, starts active and has no technical id yet")
	void createGeneratesIdAndStartsActive() {
		Customer customer = Customer.create(personalData(LocalDate.of(1990, 5, 14)), ENCODED_PASSWORD, TODAY);

		assertThat(customer.getCustomerId()).isNotNull();
		assertThat(customer.isActive()).isTrue();
		assertThat(customer.getId()).isNull();
		assertThat(customer.getPassword()).isEqualTo(ENCODED_PASSWORD);
	}

	@Test
	@DisplayName("create generates a different customer id for each customer")
	void createGeneratesUniqueIds() {
		Customer first = Customer.create(personalData(LocalDate.of(1990, 5, 14)), ENCODED_PASSWORD, TODAY);
		Customer second = Customer.create(personalData(LocalDate.of(1990, 5, 14)), ENCODED_PASSWORD, TODAY);

		assertThat(first.getCustomerId()).isNotEqualTo(second.getCustomerId());
	}

	@Test
	@DisplayName("create accepts a customer who turns 18 today")
	void createAcceptsCustomerTurningEighteenToday() {
		Customer customer = Customer.create(personalData(TODAY.minusYears(18)), ENCODED_PASSWORD, TODAY);

		assertThat(customer.ageOn(TODAY)).isEqualTo(18);
	}

	@Test
	@DisplayName("create rejects a customer who turns 18 tomorrow")
	void createRejectsUnderageCustomer() {
		PersonalData minor = personalData(TODAY.minusYears(18).plusDays(1));

		assertThatThrownBy(() -> Customer.create(minor, ENCODED_PASSWORD, TODAY))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Customer must be at least 18 years old");
	}

	@Test
	@DisplayName("create rejects a birth date in the future")
	void createRejectsFutureBirthDate() {
		PersonalData unborn = personalData(TODAY.plusDays(1));

		assertThatThrownBy(() -> Customer.create(unborn, ENCODED_PASSWORD, TODAY))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Birth date cannot be in the future");
	}

	@Test
	@DisplayName("create rejects a missing password")
	void createRejectsBlankPassword() {
		PersonalData data = personalData(LocalDate.of(1990, 5, 14));

		assertThatThrownBy(() -> Customer.create(data, " ", TODAY))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Password is required");
	}

	@Test
	@DisplayName("create rejects missing personal data")
	void createRejectsMissingPersonalData() {
		assertThatThrownBy(() -> Customer.create(null, ENCODED_PASSWORD, TODAY))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Personal data is required");
	}

	@Test
	@DisplayName("age is derived from the birth date on the given day")
	void ageIsDerivedFromBirthDate() {
		Customer customer = Customer.create(personalData(LocalDate.of(1990, 10, 23)), ENCODED_PASSWORD, TODAY);

		assertThat(customer.ageOn(TODAY)).isEqualTo(35);
		assertThat(customer.ageOn(TODAY.plusDays(1))).isEqualTo(36);
	}

	private static PersonalData personalData(LocalDate birthDate) {
		return new PersonalData("Jose Lema", Gender.MALE, birthDate, "1712345678", "Otavalo sn y principal",
				"098254785");
	}
}
