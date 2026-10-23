package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.devsu.accounts.domain.model.AccountType;

public record AccountResponse(
		String accountNumber,
		AccountType accountType,
		BigDecimal initialBalance,
		BigDecimal availableBalance,
		boolean active,
		UUID customerId) {
}
