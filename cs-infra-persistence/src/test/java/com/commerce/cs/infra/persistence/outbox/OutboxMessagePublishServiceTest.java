package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.domain.event.ReturnRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@DisplayName("OutboxMessagePublishService 발행 안정성")
@ExtendWith(MockitoExtension.class)
class OutboxMessagePublishServiceTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-23T00:01:00Z"),
        ZoneOffset.UTC
    );

    @Mock
    private OutboxJpaRepository outboxJpaRepository;

    @Mock
    private OutboxExternalPublisher externalPublisher;

    @Test
    @DisplayName("외부 발행 성공 후 PUBLISHED 상태로 저장한다")
    void publish_marks_message_published_after_external_publish() {
        OutboxMessage message = message();
        OutboxMessagePublishService service = service();

        service.publish(message);

        InOrder inOrder = inOrder(externalPublisher, outboxJpaRepository);
        inOrder.verify(externalPublisher).publish(message.eventType(), message.payload());
        inOrder.verify(outboxJpaRepository).save(message);
        assertThat(message.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.publishedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("외부 발행 실패 시 PENDING 상태를 유지하고 재시도 정보를 저장한다")
    void publish_keeps_message_pending_when_external_publish_fails() {
        OutboxMessage message = message();
        doThrow(new IllegalStateException("timeout"))
            .when(externalPublisher)
            .publish(eq(message.eventType()), eq(message.payload()));
        OutboxMessagePublishService service = service();

        service.publish(message);

        verify(outboxJpaRepository).save(message);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.lastFailedAt()).isEqualTo(CLOCK.instant());
        assertThat(message.lastFailureReason()).isEqualTo("timeout");
        assertThat(message.deadLetterReason()).isNull();
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달한 실패는 DEAD_LETTER 상태로 저장한다")
    void publish_marks_dead_letter_when_retry_limit_is_reached() {
        OutboxMessage message = messageWithRetryCount(4);
        doThrow(new IllegalStateException("broker unavailable"))
            .when(externalPublisher)
            .publish(eq(message.eventType()), eq(message.payload()));
        OutboxMessagePublishService service = service();

        service.publish(message);

        verify(outboxJpaRepository).save(message);
        assertThat(message.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(message.retryCount()).isEqualTo(5);
        assertThat(message.lastFailedAt()).isEqualTo(CLOCK.instant());
        assertThat(message.deadLetterReason()).isEqualTo("broker unavailable");
    }

    private OutboxMessagePublishService service() {
        return new OutboxMessagePublishService(outboxJpaRepository, externalPublisher, CLOCK);
    }

    private OutboxMessage messageWithRetryCount(int retryCount) {
        OutboxMessage message = message();
        for (int i = 0; i < retryCount; i++) {
            message.markPublishFailed("previous failure", CLOCK.instant());
        }
        return message;
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
