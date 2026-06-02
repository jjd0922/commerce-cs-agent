package com.commerce.cs.bootstrap.local;

import com.commerce.cs.infra.persistence.outbox.OutboxExternalPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalOutboxExternalPublisher implements OutboxExternalPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalOutboxExternalPublisher.class);

    @Override
    public void publish(String eventType, String payload) {
        log.info("Local outbox event published: eventType={}, payload={}", eventType, payload);
    }
}
