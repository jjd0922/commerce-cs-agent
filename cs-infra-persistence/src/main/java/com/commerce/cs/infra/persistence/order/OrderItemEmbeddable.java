package com.commerce.cs.infra.persistence.order;

import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.order.OrderItem;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class OrderItemEmbeddable {

    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPriceAmount;
    private String unitPriceCurrency;

    protected OrderItemEmbeddable() {
    }

    private OrderItemEmbeddable(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPriceAmount = unitPriceAmount;
        this.unitPriceCurrency = unitPriceCurrency;
    }

    public static OrderItemEmbeddable from(OrderItem item) {
        return new OrderItemEmbeddable(
            item.productId(),
            item.productName(),
            item.quantity(),
            item.unitPrice().amount(),
            item.unitPrice().currency()
        );
    }

    public OrderItem toDomain() {
        return new OrderItem(
            productId,
            productName,
            quantity,
            new Money(unitPriceAmount, unitPriceCurrency)
        );
    }
}
