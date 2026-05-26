package com.commerce.cs.infra.persistence.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class OutboxMessagePublishService {

    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxExternalPublisher externalPublisher;
    private final Clock clock;

    @Transactional
    public void publish(OutboxMessage message) {
        try {
            externalPublisher.publish(message.eventType(), message.payload());
            message.markPublished(clock.instant());
        } catch (RuntimeException e) {
            markFailure(message, e);
        }
        outboxJpaRepository.save(message);
    }

    private void markFailure(OutboxMessage message, RuntimeException cause) {
        String reason = failureReason(cause);
        if (message.retryCount() + 1 >= MAX_RETRY_COUNT) {
            message.markDeadLetter(reason, clock.instant());
            return;
        }
        message.markPublishFailed(reason, clock.instant());
    }

    private String failureReason(RuntimeException cause) {
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}
