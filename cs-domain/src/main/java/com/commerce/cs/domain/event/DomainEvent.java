package com.commerce.cs.domain.event;

import java.time.Instant;

public interface DomainEvent {

    String aggregateId();

    Instant occurredAt();
}
