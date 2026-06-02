package com.commerce.cs.infra.persistence.order;

import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.order.OrderItem;
import com.commerce.cs.domain.order.OrderStatus;
import com.commerce.cs.domain.order.PaymentStatus;
import com.commerce.cs.domain.order.ShipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderEntity 주문 도메인 변환")
class OrderEntityTest {

    @Test
    @DisplayName("주문 엔티티를 주문 도메인으로 변환한다")
    void maps_order_entity_to_domain() {
        Order order = new Order(
            "order-1",
            "user-1",
            OrderStatus.DELIVERED,
            PaymentStatus.PAID,
            ShipmentStatus.DELIVERED,
            List.of(new OrderItem("product-1", "Sneakers", 2, Money.won(50_000))),
            Instant.parse("2026-05-19T00:00:00Z"),
            Instant.parse("2026-05-20T00:00:00Z")
        );

        Order mapped = OrderEntity.from(order).toDomain();

        assertThat(mapped.id()).isEqualTo(order.id());
        assertThat(mapped.userId()).isEqualTo(order.userId());
        assertThat(mapped.status()).isEqualTo(order.status());
        assertThat(mapped.totalAmount()).isEqualTo(Money.won(100_000));
        assertThat(mapped.canRequestReturn()).isTrue();
    }
}
