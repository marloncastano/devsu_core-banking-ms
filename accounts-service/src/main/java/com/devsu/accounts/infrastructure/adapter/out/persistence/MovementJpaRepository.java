package com.devsu.accounts.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {
}
