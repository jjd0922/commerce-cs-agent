package com.commerce.cs.application.order;

import com.commerce.cs.domain.order.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findByIdAndUserId(String orderId, String userId);
}
