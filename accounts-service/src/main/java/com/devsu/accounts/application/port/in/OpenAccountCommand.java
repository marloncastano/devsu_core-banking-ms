package com.devsu.accounts.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import com.devsu.accounts.domain.model.AccountType;

public record OpenAccountCommand(
		AccountType accountType,
		BigDecimal initialBalance,
		UUID customerId) {
}
