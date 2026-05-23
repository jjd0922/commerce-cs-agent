package com.commerce.cs.application.chat;

import java.time.Instant;
import java.util.Map;

public final class PendingAction {

    private final String toolName;
    private final Map<String, Object> args;
    private final String idempotencyKey;
    private final Instant createdAt;

    public PendingAction(String toolName, Map<String, Object> args, String idempotencyKey, Instant createdAt) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.toolName = toolName;
        this.args = args == null ? Map.of() : Map.copyOf(args);
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public String toolName() {
        return toolName;
    }

    public Map<String, Object> args() {
        return args;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
