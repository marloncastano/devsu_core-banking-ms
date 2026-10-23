package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_ref")
@Getter
@Setter
@NoArgsConstructor
public class CustomerReferenceEntity {

	@Id
	@Column(name = "customer_id")
	private UUID customerId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private boolean active;

	@UpdateTimestamp
	@Setter(AccessLevel.NONE)
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
