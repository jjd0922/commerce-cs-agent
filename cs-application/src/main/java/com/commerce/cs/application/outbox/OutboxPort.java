package com.commerce.cs.application.outbox;

import com.commerce.cs.domain.event.DomainEvent;

public interface OutboxPort {

    void save(DomainEvent event);
}
