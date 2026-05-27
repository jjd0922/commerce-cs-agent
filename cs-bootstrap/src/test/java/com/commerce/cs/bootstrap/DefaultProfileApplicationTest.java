package com.commerce.cs.bootstrap;

import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.bootstrap.demo.InMemoryOutboxPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Default profile application context")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    properties = "spring.main.web-application-type=none"
)
class DefaultProfileApplicationTest {

    @Autowired
    private OutboxPort outboxPort;

    @Test
    @DisplayName("loads demo wiring when no profile is explicitly active")
    void loads_demo_wiring_without_explicit_profile() {
        assertThat(outboxPort).isInstanceOf(InMemoryOutboxPort.class);
    }
}
