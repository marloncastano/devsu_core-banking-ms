package com.devsu.accounts.application.port.out;

/**
 * Assigns account numbers. Account numbers are owned by the bank, never chosen by the client.
 */
public interface AccountNumberGeneratorPort {

	String next();
}
