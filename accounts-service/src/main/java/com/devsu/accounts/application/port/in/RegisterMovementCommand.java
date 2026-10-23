package com.devsu.accounts.application.port.in;

import java.math.BigDecimal;

/**
 * @param amount         positive for a deposit, negative for a withdrawal.
 * @param idempotencyKey optional key that makes retries safe; {@code null} when the client sends none.
 */
public record RegisterMovementCommand(String accountNumber, BigDecimal amount, String idempotencyKey) {
}
