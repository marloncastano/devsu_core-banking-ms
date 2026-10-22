package com.devsu.customers.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;

import com.devsu.customers.domain.model.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull Gender gender,
		@NotNull @Past LocalDate birthDate,
		@NotBlank @Pattern(regexp = CustomerValidation.IDENTIFICATION,
				message = CustomerValidation.IDENTIFICATION_MESSAGE) String identification,
		@NotBlank @Size(max = 200) String address,
		@NotBlank @Pattern(regexp = CustomerValidation.PHONE,
				message = CustomerValidation.PHONE_MESSAGE) String phone,
		@NotBlank @Size(min = CustomerValidation.PASSWORD_MIN_LENGTH, message = CustomerValidation.PASSWORD_MIN_MESSAGE)
				@MaxBytes(CustomerValidation.PASSWORD_MAX_BYTES) String password,
		@NotNull Boolean active) {
}
