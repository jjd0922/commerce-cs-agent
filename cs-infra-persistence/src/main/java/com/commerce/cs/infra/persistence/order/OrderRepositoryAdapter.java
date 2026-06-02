package com.commerce.cs.infra.persistence.order;

import com.commerce.cs.application.order.OrderRepository;
import com.commerce.cs.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("local")
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findByIdAndUserId(String orderId, String userId) {
        return orderJpaRepository.findByIdAndUserId(orderId, userId)
            .map(OrderEntity::toDomain);
    }
}
