package com.commerce.cs.infra.persistence.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class OutboxMessagePublishService {

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxExternalPublisher externalPublisher;
    private final Clock clock;

    @Transactional
    public void publish(OutboxMessage message) {
        try {
            externalPublisher.publish(message.eventType(), message.payload());
            message.markPublished(clock.instant());
        } catch (RuntimeException e) {
            message.incrementRetryCount();
        }
        outboxJpaRepository.save(message);
    }
}
