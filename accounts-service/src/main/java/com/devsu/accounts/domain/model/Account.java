package com.devsu.accounts.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.devsu.accounts.domain.exception.AccountNotAvailableException;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.DomainValidationException;
import com.devsu.accounts.domain.exception.InsufficientFundsException;

public class Account {

	static final int MONEY_SCALE = 2;

	private final Long id;
	private final String accountNumber;
	private final AccountType accountType;
	private final BigDecimal initialBalance;
	private final UUID customerId;
	private BigDecimal availableBalance;
	private boolean active;

	private Account(Long id, String accountNumber, AccountType accountType, BigDecimal initialBalance,
			BigDecimal availableBalance, boolean active, UUID customerId) {
		if (accountNumber == null || accountNumber.isBlank()) {
			throw new DomainValidationException("Account number is required");
		}
		if (accountType == null) {
			throw new DomainValidationException("Account type is required");
		}
		if (customerId == null) {
			throw new DomainValidationException("Customer id is required");
		}
		this.id = id;
		this.accountNumber = accountNumber.strip();
		this.accountType = accountType;
		this.initialBalance = toMoney(initialBalance, "Initial balance");
		this.availableBalance = toMoney(availableBalance, "Available balance");
		this.active = active;
		this.customerId = customerId;
	}

	/**
	 * Opens a new, active account for an active customer. The available balance starts at the initial balance.
	 */
	public static Account open(String accountNumber, AccountType accountType, BigDecimal initialBalance,
			CustomerReference customer) {
		if (customer == null) {
			throw new DomainValidationException("Customer is required");
		}
		if (!customer.active()) {
			throw CustomerNotAvailableException.inactive(customer.customerId());
		}
		return new Account(null, accountNumber, accountType, initialBalance, initialBalance, true,
				customer.customerId());
	}

	/**
	 * Rebuilds an existing account from its persisted state.
	 */
	public static Account restore(Long id, String accountNumber, AccountType accountType, BigDecimal initialBalance,
			BigDecimal availableBalance, boolean active, UUID customerId) {
		return new Account(id, accountNumber, accountType, initialBalance, availableBalance, active, customerId);
	}

	/**
	 * Applies a deposit (positive amount) or a withdrawal (negative amount) and returns the resulting ledger entry.
	 * The balance can never become negative.
	 */
	public Movement registerMovement(BigDecimal amount, Instant occurredAt) {
		if (!active) {
			throw AccountNotAvailableException.inactive(accountNumber);
		}
		if (amount == null || amount.signum() == 0) {
			throw new DomainValidationException("Amount must be different from zero");
		}
		if (amount.stripTrailingZeros().scale() > MONEY_SCALE) {
			throw new DomainValidationException("Amount cannot have more than " + MONEY_SCALE + " decimals");
		}
		BigDecimal newBalance = availableBalance.add(amount);
		if (newBalance.signum() < 0) {
			throw new InsufficientFundsException();
		}
		this.availableBalance = newBalance.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
		return new Movement(UUID.randomUUID(), id, accountNumber, MovementType.of(amount),
				amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY), availableBalance, occurredAt);
	}

	private static BigDecimal toMoney(BigDecimal amount, String field) {
		if (amount == null) {
			throw new DomainValidationException(field + " is required");
		}
		if (amount.signum() < 0) {
			throw new DomainValidationException(field + " cannot be negative");
		}
		if (amount.stripTrailingZeros().scale() > MONEY_SCALE) {
			throw new DomainValidationException(field + " cannot have more than " + MONEY_SCALE + " decimals");
		}
		return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
	}

	public Long getId() {
		return id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public BigDecimal getInitialBalance() {
		return initialBalance;
	}

	public BigDecimal getAvailableBalance() {
		return availableBalance;
	}

	public boolean isActive() {
		return active;
	}

	public UUID getCustomerId() {
		return customerId;
	}
}
