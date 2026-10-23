package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.devsu.accounts.domain.model.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_number", nullable = false, unique = true, updatable = false, length = 20)
	private String accountNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, updatable = false, length = 10)
	private AccountType accountType;

	@Column(name = "initial_balance", nullable = false, updatable = false, precision = 15, scale = 2)
	private BigDecimal initialBalance;

	@Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal availableBalance;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "customer_id", nullable = false, updatable = false)
	private UUID customerId;

	/**
	 * Optimistic locking: concurrent updates of the same account fail instead of overwriting each other.
	 */
	@Version
	@Setter(AccessLevel.NONE)
	private Long version;

	@CreationTimestamp
	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Setter(AccessLevel.NONE)
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
