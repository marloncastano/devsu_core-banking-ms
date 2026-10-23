package com.devsu.accounts.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.devsu.accounts.application.port.out.AccountRepositoryPort;
import com.devsu.accounts.domain.model.Account;

@Component
class AccountPersistenceAdapter implements AccountRepositoryPort {

	private final AccountJpaRepository accountJpaRepository;

	AccountPersistenceAdapter(AccountJpaRepository accountJpaRepository) {
		this.accountJpaRepository = accountJpaRepository;
	}

	/**
	 * Updates load the managed entity and copy only mutable fields, so audit and version columns stay consistent.
	 */
	@Override
	public Account save(Account account) {
		AccountEntity entity = account.getId() == null
				? newEntity(account)
				: accountJpaRepository.findById(account.getId())
						.orElseThrow(() -> new IllegalStateException("Account to update does not exist: "
								+ account.getAccountNumber()));
		entity.setAvailableBalance(account.getAvailableBalance());
		entity.setActive(account.isActive());
		return toDomain(accountJpaRepository.save(entity));
	}

	private static AccountEntity newEntity(Account account) {
		AccountEntity entity = new AccountEntity();
		entity.setAccountNumber(account.getAccountNumber());
		entity.setAccountType(account.getAccountType());
		entity.setInitialBalance(account.getInitialBalance());
		entity.setCustomerId(account.getCustomerId());
		return entity;
	}

	private static Account toDomain(AccountEntity entity) {
		return Account.restore(entity.getId(), entity.getAccountNumber(), entity.getAccountType(),
				entity.getInitialBalance(), entity.getAvailableBalance(), entity.isActive(), entity.getCustomerId());
	}
}
