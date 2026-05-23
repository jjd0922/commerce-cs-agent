package com.commerce.cs.domain.event;

import java.time.Instant;

public record ReturnRequestedEvent(
    String returnId,
    String orderId,
    String userId,
    Instant occurredAt
) implements DomainEvent {

    public ReturnRequestedEvent {
        if (returnId == null || returnId.isBlank()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }

    @Override
    public String aggregateId() {
        return returnId;
    }
}
