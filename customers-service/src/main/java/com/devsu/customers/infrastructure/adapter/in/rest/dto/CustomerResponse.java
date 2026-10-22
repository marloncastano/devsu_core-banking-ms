package com.devsu.customers.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.devsu.customers.domain.model.Gender;

public record CustomerResponse(
		UUID customerId,
		String name,
		Gender gender,
		LocalDate birthDate,
		int age,
		String identification,
		String address,
		String phone,
		boolean active) {
}
