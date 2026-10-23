package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param amount positive for a deposit, negative for a withdrawal.
 */
public record RegisterMovementRequest(
		@NotBlank String accountNumber,
		@NotNull @Digits(integer = 13, fraction = 2) BigDecimal amount) {
}
