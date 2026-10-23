package com.devsu.accounts.application.port.in;

import com.devsu.accounts.domain.model.Movement;

/**
 * @param replayed {@code true} when the request was a retry and the original movement is returned without being
 *                 applied again.
 */
public record MovementRegistration(Movement movement, boolean replayed) {
}
