package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Clock;

@Repository
@RequiredArgsConstructor
public class OutboxAdapter implements OutboxPort {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void save(DomainEvent event) {
        outboxJpaRepository.save(OutboxMessage.from(event, serialize(event), clock.instant()));
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
