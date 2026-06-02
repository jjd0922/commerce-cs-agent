package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.domain.event.ReturnRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxMessage 상태 전이")
class OutboxMessageTest {

    @Test
    @DisplayName("도메인 이벤트를 PENDING outbox 메시지로 생성한다")
    void creates_pending_outbox_message_from_domain_event() {
        ReturnRequestedEvent event = new ReturnRequestedEvent(
            "return-1",
            "order-1",
            "user-1",
            Instant.parse("2026-05-23T00:00:00Z")
        );

        OutboxMessage message = OutboxMessage.from(event, "{\"returnId\":\"return-1\"}", Instant.parse("2026-05-23T00:00:01Z"));

        assertThat(message.eventType()).isEqualTo(ReturnRequestedEvent.class.getName());
        assertThat(message.payload()).contains("return-1");
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.retryCount()).isZero();
    }

    @Test
    @DisplayName("메시지 발행 성공 시 PUBLISHED 상태로 변경한다")
    void marks_message_as_published() {
        OutboxMessage message = message();

        message.markPublished(Instant.parse("2026-05-23T00:01:00Z"));

        assertThat(message.status()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("재시도 횟수를 증가시키고 DEAD_LETTER 상태로 변경한다")
    void increments_retry_and_marks_dead_letter() {
        OutboxMessage message = message();

        message.incrementRetryCount();
        message.markDeadLetter();

        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
    }

    private OutboxMessage message() {
        ReturnRequestedEvent event = new ReturnRequestedEvent(
            "return-1",
            "order-1",
            "user-1",
            Instant.parse("2026-05-23T00:00:00Z")
        );
        return OutboxMessage.from(event, "{}", Instant.parse("2026-05-23T00:00:01Z"));
    }
}
