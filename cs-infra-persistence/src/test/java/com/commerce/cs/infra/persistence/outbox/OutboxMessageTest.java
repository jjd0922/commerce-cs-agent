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
        Instant publishedAt = Instant.parse("2026-05-23T00:01:00Z");

        message.markPublished(publishedAt);

        assertThat(message.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.publishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("발행 실패 시 재시도 횟수와 실패 사유를 기록한다")
    void marks_publish_failure() {
        OutboxMessage message = message();
        Instant failedAt = Instant.parse("2026-05-23T00:02:00Z");

        message.markPublishFailed("timeout", failedAt);

        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.lastFailedAt()).isEqualTo(failedAt);
        assertThat(message.lastFailureReason()).isEqualTo("timeout");
        assertThat(message.deadLetterReason()).isNull();
    }

    @Test
    @DisplayName("재시도 한계 도달 시 DEAD_LETTER 상태와 사유를 기록한다")
    void marks_dead_letter_with_reason() {
        OutboxMessage message = message();
        Instant failedAt = Instant.parse("2026-05-23T00:03:00Z");

        message.markDeadLetter("max retry exceeded", failedAt);

        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(message.lastFailedAt()).isEqualTo(failedAt);
        assertThat(message.lastFailureReason()).isEqualTo("max retry exceeded");
        assertThat(message.deadLetterReason()).isEqualTo("max retry exceeded");
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
