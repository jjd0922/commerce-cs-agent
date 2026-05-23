package com.commerce.cs.infra.persistence.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int POLL_LIMIT = 100;
    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxExternalPublisher externalPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void poll() {
        List<OutboxMessage> messages = outboxJpaRepository.findPendingForUpdate(PageRequest.of(0, POLL_LIMIT));
        messages.forEach(this::publish);
    }

    private void publish(OutboxMessage message) {
        try {
            externalPublisher.publish(message.eventType(), message.payload());
            message.markPublished(clock.instant());
        } catch (RuntimeException e) {
            message.incrementRetryCount();
            if (message.retryCount() >= MAX_RETRY_COUNT) {
                message.markDeadLetter();
            }
        }
        outboxJpaRepository.save(message);
    }
}
