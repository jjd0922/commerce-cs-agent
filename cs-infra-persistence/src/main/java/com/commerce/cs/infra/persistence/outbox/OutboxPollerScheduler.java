package com.commerce.cs.infra.persistence.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
@Conditional(OutboxPollerEnabledCondition.class)
public class OutboxPollerScheduler {

    private final OutboxPoller outboxPoller;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        outboxPoller.poll();
    }
}
