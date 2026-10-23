package com.devsu.accounts.application.port.in;

public interface SyncCustomerUseCase {

	/**
	 * Applies a customer change to the local copy. Idempotent: replaying the same or an older change has no effect.
	 */
	void sync(SyncCustomerCommand command);
}
