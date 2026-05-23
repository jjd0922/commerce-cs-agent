package com.commerce.cs.domain.returns;

import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.order.Order;

import java.time.Duration;
import java.time.Instant;

public final class ReturnPolicy {

    private static final Duration DEFAULT_RETURN_WINDOW = Duration.ofDays(7);
    private static final Duration DEFAULT_REFUND_DELAY = Duration.ofDays(3);

    private final Duration returnWindow;
    private final Duration refundDelay;

    public ReturnPolicy() {
        this(DEFAULT_RETURN_WINDOW, DEFAULT_REFUND_DELAY);
    }

    public ReturnPolicy(Duration returnWindow, Duration refundDelay) {
        if (returnWindow == null || returnWindow.isNegative() || returnWindow.isZero()) {
            throw new IllegalArgumentException("returnWindow must be positive");
        }
        if (refundDelay == null || refundDelay.isNegative()) {
            throw new IllegalArgumentException("refundDelay must not be negative");
        }
        this.returnWindow = returnWindow;
        this.refundDelay = refundDelay;
    }

    public boolean isReturnable(Order order, Instant requestedAt) {
        if (order == null || requestedAt == null || !order.canRequestReturn()) {
            return false;
        }
        Instant deadline = order.deliveredAt().plus(returnWindow);
        return !requestedAt.isAfter(deadline);
    }

    public void validate(Order order, ReturnReason reason, Instant requestedAt) {
        if (!isReturnable(order, requestedAt)) {
            throw new IllegalArgumentException("Order is not returnable");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
    }

    public boolean reasonRequiresEvidence(ReturnReason reason) {
        return reason == ReturnReason.DEFECT || reason == ReturnReason.WRONG_ITEM;
    }

    public Money refundAmount(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        return order.totalAmount();
    }

    public Instant estimateRefundAt(Instant requestedAt) {
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
        return requestedAt.plus(refundDelay);
    }
}
