package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.domain.event.ReturnRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OutboxPoller 메시지 발행 처리")
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-23T00:01:00Z"),
        ZoneOffset.UTC
    );

    @Mock
    private OutboxJpaRepository outboxJpaRepository;

    @Mock
    private OutboxExternalPublisher externalPublisher;

    @Test
    @DisplayName("PENDING 메시지를 조회해 외부 발행 후 PUBLISHED 상태로 저장한다")
    void poll_publishes_pending_message_and_marks_published() {
        OutboxMessage message = message();
        when(outboxJpaRepository.findPendingForUpdate(any(Pageable.class))).thenReturn(List.of(message));
        OutboxPoller poller = new OutboxPoller(outboxJpaRepository, externalPublisher, CLOCK);

        poller.poll();

        verify(externalPublisher).publish(message.eventType(), message.payload());
        verify(outboxJpaRepository).save(message);
        assertThat(message.status()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("외부 발행 실패 시 재시도 횟수를 증가시키고 메시지를 저장한다")
    void poll_increments_retry_count_when_publish_fails() {
        OutboxMessage message = message();
        when(outboxJpaRepository.findPendingForUpdate(any(Pageable.class))).thenReturn(List.of(message));
        doThrow(new IllegalStateException("publish failed"))
            .when(externalPublisher)
            .publish(eq(message.eventType()), eq(message.payload()));
        OutboxPoller poller = new OutboxPoller(outboxJpaRepository, externalPublisher, CLOCK);

        poller.poll();

        verify(outboxJpaRepository).save(message);
        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
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
