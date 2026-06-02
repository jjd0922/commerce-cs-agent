package com.commerce.cs.infra.persistence.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int POLL_LIMIT = 100;

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxMessagePublishService publishService;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void poll() {
        List<OutboxMessage> messages = outboxJpaRepository.findPendingForUpdate(PageRequest.of(0, POLL_LIMIT));
        messages.forEach(publishService::publish);
    }
}
