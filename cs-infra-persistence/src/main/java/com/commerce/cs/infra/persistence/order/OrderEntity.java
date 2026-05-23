package com.commerce.cs.infra.persistence.order;

import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.order.OrderItem;
import com.commerce.cs.domain.order.OrderStatus;
import com.commerce.cs.domain.order.PaymentStatus;
import com.commerce.cs.domain.order.ShipmentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;

    private String userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus shipmentStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItemEmbeddable> items = new ArrayList<>();

    private Instant orderedAt;
    private Instant deliveredAt;

    protected OrderEntity() {
    }

    public static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.id();
        entity.userId = order.userId();
        entity.status = order.status();
        entity.paymentStatus = order.paymentStatus();
        entity.shipmentStatus = order.shipmentStatus();
        entity.items = order.items().stream()
            .map(OrderItemEmbeddable::from)
            .toList();
        entity.orderedAt = order.orderedAt();
        entity.deliveredAt = order.deliveredAt();
        return entity;
    }

    public Order toDomain() {
        List<OrderItem> domainItems = items.stream()
            .map(OrderItemEmbeddable::toDomain)
            .toList();
        return new Order(
            id,
            userId,
            status,
            paymentStatus,
            shipmentStatus,
            domainItems,
            orderedAt,
            deliveredAt
        );
    }
}
