package com.commerce.cs.infra.persistence.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnJpaRepository extends JpaRepository<ReturnEntity, String> {

    Optional<ReturnEntity> findByIdempotencyKey(String idempotencyKey);
}
