package com.commerce.cs.application.returns;

import com.commerce.cs.domain.returns.ReturnReason;

public final class ReturnCommand {

    private final String orderId;
    private final String userId;
    private final ReturnReason reason;
    private final String detail;
    private final String idempotencyKey;

    public ReturnCommand(String orderId, String userId, ReturnReason reason, String detail, String idempotencyKey) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.detail = detail;
        this.idempotencyKey = idempotencyKey;
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

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
