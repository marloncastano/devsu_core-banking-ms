package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.devsu.accounts.domain.model.AccountType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
		@NotNull AccountType accountType,
		@NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal initialBalance,
		@NotNull UUID customerId) {
}
