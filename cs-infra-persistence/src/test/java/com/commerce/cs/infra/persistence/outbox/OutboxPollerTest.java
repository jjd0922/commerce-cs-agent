package com.commerce.cs.infra.persistence.outbox;

import com.commerce.cs.domain.event.ReturnRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("OutboxPoller 메시지 발행 처리")
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxJpaRepository outboxJpaRepository;

    @Mock
    private OutboxMessagePublishService publishService;

    @Test
    @DisplayName("PENDING 메시지를 조회해 발행 서비스에 위임한다")
    void poll_delegates_pending_messages_to_publish_service() {
        OutboxMessage message = message();
        when(outboxJpaRepository.findPendingForUpdate(any(Pageable.class))).thenReturn(List.of(message));
        OutboxPoller poller = new OutboxPoller(outboxJpaRepository, publishService);

        poller.poll();

        verify(publishService).publish(message);
    }

    @Test
    @DisplayName("PENDING 메시지가 없으면 발행 서비스가 호출되지 않는다")
    void poll_does_not_call_publish_service_when_no_message_exists() {
        when(outboxJpaRepository.findPendingForUpdate(any(Pageable.class))).thenReturn(List.of());
        OutboxPoller poller = new OutboxPoller(outboxJpaRepository, publishService);

        poller.poll();

        verifyNoInteractions(publishService);
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
