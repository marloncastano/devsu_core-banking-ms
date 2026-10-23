package com.devsu.accounts.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.devsu.accounts.application.port.out.AccountNumberGeneratorPort;

/**
 * Backed by a PostgreSQL sequence, so numbers are unique across concurrent requests and service instances.
 */
@Component
class AccountNumberSequenceAdapter implements AccountNumberGeneratorPort {

	private final AccountJpaRepository accountJpaRepository;

	AccountNumberSequenceAdapter(AccountJpaRepository accountJpaRepository) {
		this.accountJpaRepository = accountJpaRepository;
	}

	@Override
	public String next() {
		return String.valueOf(accountJpaRepository.nextAccountNumber());
	}
}
