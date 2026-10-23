package com.devsu.accounts.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

	@Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
	long nextAccountNumber();
}
