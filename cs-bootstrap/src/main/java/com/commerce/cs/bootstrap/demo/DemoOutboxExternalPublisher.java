package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.infra.persistence.outbox.OutboxExternalPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoOutboxExternalPublisher implements OutboxExternalPublisher {

    private static final Logger log = LoggerFactory.getLogger(DemoOutboxExternalPublisher.class);

    @Override
    public void publish(String eventType, String payload) {
        log.info("Demo outbox publish eventType={}, payload={}", eventType, payload);
    }
}
