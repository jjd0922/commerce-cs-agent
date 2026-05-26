package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.domain.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    private String id;

    private String aggregateId;
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int retryCount;
    private Instant occurredAt;
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxMessage() {
    }

    private OutboxMessage(
        String id,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int retryCount,
        Instant occurredAt,
        Instant createdAt,
        Instant publishedAt
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public static OutboxMessage from(DomainEvent event, String payload, Instant createdAt) {
        return new OutboxMessage(
            UUID.randomUUID().toString(),
            event.aggregateId(),
            event.getClass().getName(),
            payload,
            OutboxStatus.PENDING,
            0,
            event.occurredAt(),
            createdAt,
            null
        );
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markDeadLetter() {
        this.status = OutboxStatus.DEAD_LETTER;
    }

    public String eventType() {
        return eventType;
    }

    public String payload() {
        return payload;
    }

    public OutboxStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
    }
}
