package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Only the status can change. Number, type, owner and balances are immutable; the balance only changes through
 * movements.
 */
public record PatchAccountRequest(@NotNull Boolean active) {
}
