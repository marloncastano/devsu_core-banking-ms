package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.devsu.accounts.domain.model.MovementType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ledger entries are insert-only: every column is non-updatable.
 */
@Entity
@Table(name = "movement")
@Getter
@Setter
@NoArgsConstructor
public class MovementEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "movement_id", nullable = false, unique = true, updatable = false)
	private UUID movementId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private AccountEntity account;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, updatable = false, length = 10)
	private MovementType movementType;

	@Column(nullable = false, updatable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false, precision = 15, scale = 2)
	private BigDecimal balance;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@CreationTimestamp
	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
