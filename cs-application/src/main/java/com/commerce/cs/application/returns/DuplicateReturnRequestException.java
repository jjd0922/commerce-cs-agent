package com.commerce.cs.application.returns;

public class DuplicateReturnRequestException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicateReturnRequestException(String idempotencyKey, Throwable cause) {
        super("Duplicate return request idempotency key: " + idempotencyKey, cause);
        this.idempotencyKey = idempotencyKey;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
