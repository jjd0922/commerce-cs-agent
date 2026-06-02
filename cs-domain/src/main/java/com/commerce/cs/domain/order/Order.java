package com.commerce.cs.domain.order;

import com.commerce.cs.domain.common.Money;

import java.time.Instant;
import java.util.List;

public final class Order {

    private final String id;
    private final String userId;
    private final OrderStatus status;
    private final PaymentStatus paymentStatus;
    private final ShipmentStatus shipmentStatus;
    private final List<OrderItem> items;
    private final Instant orderedAt;
    private final Instant deliveredAt;

    public Order(
        String id,
        String userId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        ShipmentStatus shipmentStatus,
        List<OrderItem> items,
        Instant orderedAt,
        Instant deliveredAt
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        this.id = id;
        this.userId = userId;
        this.status = requireNonNull(status, "status");
        this.paymentStatus = requireNonNull(paymentStatus, "paymentStatus");
        this.shipmentStatus = requireNonNull(shipmentStatus, "shipmentStatus");
        this.items = List.copyOf(items);
        this.orderedAt = requireNonNull(orderedAt, "orderedAt");
        this.deliveredAt = deliveredAt;
    }

    public boolean canRequestReturn() {
        return status == OrderStatus.DELIVERED
            && paymentStatus == PaymentStatus.PAID
            && shipmentStatus == ShipmentStatus.DELIVERED
            && deliveredAt != null;
    }

    public Money totalAmount() {
        return items.stream()
            .map(OrderItem::lineAmount)
            .reduce(Money.won(0), Money::add);
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public OrderStatus status() {
        return status;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public ShipmentStatus shipmentStatus() {
        return shipmentStatus;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Instant orderedAt() {
        return orderedAt;
    }

    public Instant deliveredAt() {
        return deliveredAt;
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
