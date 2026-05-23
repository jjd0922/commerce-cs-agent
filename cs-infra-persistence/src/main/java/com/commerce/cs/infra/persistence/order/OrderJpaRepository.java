package com.commerce.cs.infra.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

    Optional<OrderEntity> findByIdAndUserId(String id, String userId);
}
