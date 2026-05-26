package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryOutboxPort implements OutboxPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryOutboxPort.class);

    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void save(DomainEvent event) {
        events.add(event);
        log.info("Demo outbox event saved: {}", event);
    }

    public List<DomainEvent> events() {
        return List.copyOf(events);
    }
}
