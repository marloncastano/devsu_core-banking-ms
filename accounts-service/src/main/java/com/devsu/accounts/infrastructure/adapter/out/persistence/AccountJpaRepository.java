package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

	Optional<AccountEntity> findByAccountNumber(String accountNumber);

	Page<AccountEntity> findByCustomerId(UUID customerId, Pageable pageable);

	@Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
	long nextAccountNumber();
}
