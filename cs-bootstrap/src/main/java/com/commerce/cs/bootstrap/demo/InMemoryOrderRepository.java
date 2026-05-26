package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.order.OrderRepository;
import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.order.OrderItem;
import com.commerce.cs.domain.order.OrderStatus;
import com.commerce.cs.domain.order.PaymentStatus;
import com.commerce.cs.domain.order.ShipmentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("demo")
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public InMemoryOrderRepository() {
        save(new Order(
            "order-1",
            "user-1",
            OrderStatus.DELIVERED,
            PaymentStatus.PAID,
            ShipmentStatus.DELIVERED,
            List.of(new OrderItem("product-1", "Demo Sneakers", 2, Money.won(50_000))),
            Instant.parse("2026-05-19T00:00:00Z"),
            Instant.parse("2026-05-20T00:00:00Z")
        ));
        save(new Order(
            "order-2",
            "user-1",
            OrderStatus.SHIPPED,
            PaymentStatus.PAID,
            ShipmentStatus.IN_TRANSIT,
            List.of(new OrderItem("product-2", "Demo Backpack", 1, Money.won(80_000))),
            Instant.parse("2026-05-21T00:00:00Z"),
            null
        ));
    }

    @Override
    public Optional<Order> findByIdAndUserId(String orderId, String userId) {
        return Optional.ofNullable(orders.get(key(orderId, userId)));
    }

    private void save(Order order) {
        orders.put(key(order.id(), order.userId()), order);
    }

    private String key(String orderId, String userId) {
        return orderId + ":" + userId;
    }
}
