package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;

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

	@Override
	public Optional<Account> findByAccountNumber(String accountNumber) {
		return accountJpaRepository.findByAccountNumber(accountNumber).map(AccountPersistenceAdapter::toDomain);
	}

	/**
	 * Sorted by technical id so pages are stable: the same account never appears on two pages.
	 */
	@Override
	public PageResult<Account> findAll(PageQuery query, UUID customerId) {
		Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "id"));
		Page<AccountEntity> page = customerId == null
				? accountJpaRepository.findAll(pageable)
				: accountJpaRepository.findByCustomerId(customerId, pageable);
		return new PageResult<>(page.getContent(), query.page(), query.size(), page.getTotalElements())
				.map(AccountPersistenceAdapter::toDomain);
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
