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
    private Instant lastFailedAt;

    @Column(length = 1000)
    private String lastFailureReason;

    @Column(length = 1000)
    private String deadLetterReason;

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
        Instant publishedAt,
        Instant lastFailedAt,
        String lastFailureReason,
        String deadLetterReason
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
        this.lastFailedAt = lastFailedAt;
        this.lastFailureReason = lastFailureReason;
        this.deadLetterReason = deadLetterReason;
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
            null,
            null,
            null,
            null
        );
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void markPublishFailed(String reason, Instant failedAt) {
        this.retryCount++;
        this.lastFailedAt = failedAt;
        this.lastFailureReason = reason;
    }

    public void markDeadLetter(String reason, Instant failedAt) {
        this.retryCount++;
        this.lastFailedAt = failedAt;
        this.lastFailureReason = reason;
        this.deadLetterReason = reason;
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

    public Instant publishedAt() {
        return publishedAt;
    }

    public Instant lastFailedAt() {
        return lastFailedAt;
    }

    public String lastFailureReason() {
        return lastFailureReason;
    }

    public String deadLetterReason() {
        return deadLetterReason;
    }
}
