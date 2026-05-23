package com.commerce.cs.domain.returns;

import java.time.Instant;

public final class Return {

    private final String id;
    private final String orderId;
    private final String userId;
    private final ReturnReason reason;
    private final String detail;
    private final Instant requestedAt;
    private ReturnStatus status;

    private Return(
        String id,
        String orderId,
        String userId,
        ReturnReason reason,
        String detail,
        Instant requestedAt,
        ReturnStatus status
    ) {
        this.id = requireText(id, "id");
        this.orderId = requireText(orderId, "orderId");
        this.userId = requireText(userId, "userId");
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
        this.reason = reason;
        this.detail = detail;
        this.requestedAt = requestedAt;
        this.status = status == null ? ReturnStatus.REQUESTED : status;
    }

    public static Return request(
        String id,
        String orderId,
        String userId,
        ReturnReason reason,
        String detail,
        Instant requestedAt
    ) {
        return new Return(id, orderId, userId, reason, detail, requestedAt, ReturnStatus.REQUESTED);
    }

    public static Return restore(
        String id,
        String orderId,
        String userId,
        ReturnReason reason,
        String detail,
        Instant requestedAt,
        ReturnStatus status
    ) {
        return new Return(id, orderId, userId, reason, detail, requestedAt, status);
    }

    public void transitionTo(ReturnStatus targetStatus) {
        ReturnStateMachine.assertTransition(status, targetStatus);
        status = targetStatus;
    }

    public String id() {
        return id;
    }

    public String orderId() {
        return orderId;
    }

    public String userId() {
        return userId;
    }

    public ReturnReason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public ReturnStatus status() {
        return status;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
