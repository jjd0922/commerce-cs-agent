package com.commerce.cs.bootstrap;

import com.commerce.cs.domain.event.ReturnRequestedEvent;
import com.commerce.cs.infra.persistence.outbox.OutboxExternalPublisher;
import com.commerce.cs.infra.persistence.outbox.OutboxJpaRepository;
import com.commerce.cs.infra.persistence.outbox.OutboxMessage;
import com.commerce.cs.infra.persistence.outbox.OutboxPoller;
import com.commerce.cs.infra.persistence.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("OutboxPoller 동시 polling 처리")
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("local")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.task.scheduling.enabled=false"
)
class OutboxPollerConcurrencyTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("commerce_cs")
        .withUsername("commerce")
        .withPassword("commerce");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @MockBean
    private OutboxExternalPublisher externalPublisher;

    @Test
    @DisplayName("동시에 poll을 수행해도 같은 PENDING 메시지는 한 번만 발행한다")
    void concurrent_poll_publishes_same_pending_message_once() throws Exception {
        outboxJpaRepository.deleteAll();
        outboxJpaRepository.saveAndFlush(message());

        CountDownLatch firstPublishStarted = new CountDownLatch(1);
        CountDownLatch finishFirstPublish = new CountDownLatch(1);
        AtomicInteger publishCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (publishCount.incrementAndGet() == 1) {
                firstPublishStarted.countDown();
                assertThat(finishFirstPublish.await(2, TimeUnit.SECONDS)).isTrue();
            }
            return null;
        }).when(externalPublisher).publish(anyString(), anyString());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> firstPoll = CompletableFuture.runAsync(outboxPoller::poll, executor);
            assertThat(firstPublishStarted.await(2, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> secondPoll = CompletableFuture.runAsync(outboxPoller::poll, executor);

            TimeUnit.MILLISECONDS.sleep(150);
            assertThat(publishCount).hasValue(1);

            finishFirstPublish.countDown();
            firstPoll.get(2, TimeUnit.SECONDS);
            secondPoll.get(2, TimeUnit.SECONDS);
        } finally {
            finishFirstPublish.countDown();
            executor.shutdownNow();
        }

        verify(externalPublisher, times(1)).publish(anyString(), anyString());
        assertThat(outboxJpaRepository.findAll())
            .singleElement()
            .extracting(OutboxMessage::status)
            .isEqualTo(OutboxStatus.PUBLISHED);
    }

    private OutboxMessage message() {
        ReturnRequestedEvent event = new ReturnRequestedEvent(
            "return-1",
            "order-1",
            "user-1",
            Instant.parse("2026-05-23T00:00:00Z")
        );
        return OutboxMessage.from(event, "{\"returnId\":\"return-1\"}", Instant.parse("2026-05-23T00:00:01Z"));
    }
}
