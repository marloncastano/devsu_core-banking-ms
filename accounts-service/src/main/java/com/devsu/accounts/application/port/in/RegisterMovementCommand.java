package com.devsu.accounts.application.port.in;

import java.math.BigDecimal;

/**
 * @param amount positive for a deposit, negative for a withdrawal.
 */
public record RegisterMovementCommand(String accountNumber, BigDecimal amount) {
}
