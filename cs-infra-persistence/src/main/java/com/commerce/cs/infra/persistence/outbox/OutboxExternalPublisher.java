package com.commerce.cs.infra.persistence.outbox;

public interface OutboxExternalPublisher {

    void publish(String eventType, String payload);
}
