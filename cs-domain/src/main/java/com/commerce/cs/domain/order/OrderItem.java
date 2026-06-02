package com.commerce.cs.domain.order;

import com.commerce.cs.domain.common.Money;

public final class OrderItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(String productId, String productName, int quantity, Money unitPrice) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice must not be null");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money lineAmount() {
        return unitPrice.multiply(quantity);
    }

    public String productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public int quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }
}
