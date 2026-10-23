package com.devsu.accounts.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

	Optional<AccountEntity> findByAccountNumber(String accountNumber);

	/**
	 * SELECT ... FOR UPDATE: other transactions wait until this one ends before touching the same account.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM AccountEntity a WHERE a.accountNumber = :accountNumber")
	Optional<AccountEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

	Page<AccountEntity> findByCustomerId(UUID customerId, Pageable pageable);

	List<AccountEntity> findByCustomerIdOrderByIdAsc(UUID customerId);

	@Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
	long nextAccountNumber();
}
