package com.commerce.cs.infra.persistence.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
