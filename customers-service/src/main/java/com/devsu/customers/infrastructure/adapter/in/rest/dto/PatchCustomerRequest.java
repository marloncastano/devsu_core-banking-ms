package com.devsu.customers.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;

import com.devsu.customers.domain.model.Gender;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Partial update: every field is optional, but must be valid when present.
 */
public record PatchCustomerRequest(
		@Size(min = 1, max = 100) String name,
		Gender gender,
		@Past LocalDate birthDate,
		@Pattern(regexp = CustomerValidation.IDENTIFICATION,
				message = CustomerValidation.IDENTIFICATION_MESSAGE) String identification,
		@Size(min = 1, max = 200) String address,
		@Pattern(regexp = CustomerValidation.PHONE,
				message = CustomerValidation.PHONE_MESSAGE) String phone,
		@Size(min = CustomerValidation.PASSWORD_MIN_LENGTH, message = CustomerValidation.PASSWORD_MIN_MESSAGE)
				@MaxBytes(CustomerValidation.PASSWORD_MAX_BYTES) String password,
		Boolean active) {
}
