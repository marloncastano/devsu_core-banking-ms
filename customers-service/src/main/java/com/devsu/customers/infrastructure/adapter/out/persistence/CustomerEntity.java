package com.devsu.customers.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer")
@PrimaryKeyJoinColumn(name = "person_id")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity extends PersonEntity {

	@Column(name = "customer_id", nullable = false, unique = true, updatable = false)
	private UUID customerId;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false)
	private boolean active;
}
