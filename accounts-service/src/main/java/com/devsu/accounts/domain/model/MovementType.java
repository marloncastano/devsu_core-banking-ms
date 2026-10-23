package com.devsu.accounts.domain.model;

import java.math.BigDecimal;

public enum MovementType {
	DEPOSIT,
	WITHDRAWAL;

	/**
	 * Positive amounts are deposits and negative amounts are withdrawals.
	 */
	public static MovementType of(BigDecimal amount) {
		return amount.signum() > 0 ? DEPOSIT : WITHDRAWAL;
	}
}
