package com.commerce.cs.application.returns;

import com.commerce.cs.application.order.OrderRepository;
import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.event.ReturnRequestedEvent;
import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.order.OrderItem;
import com.commerce.cs.domain.order.OrderStatus;
import com.commerce.cs.domain.order.PaymentStatus;
import com.commerce.cs.domain.order.ShipmentStatus;
import com.commerce.cs.domain.returns.Return;
import com.commerce.cs.domain.returns.ReturnPolicy;
import com.commerce.cs.domain.returns.ReturnReason;
import com.commerce.cs.domain.returns.ReturnStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReturnService 반품 신청과 Outbox 처리 흐름")
@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-23T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private OutboxPort outboxPort;

    @Test
    @DisplayName("반품 신청이 가능하면 Return을 저장하고 ReturnRequestedEvent를 Outbox에 저장한다")
    void request_return_creates_return_and_outbox_event() {
        when(returnRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(deliveredOrder()));
        ReturnUseCase useCase = useCase();

        ReturnResult result = useCase.requestReturn(command("idem-1"));

        assertThat(result.status()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(result.refundAmount()).isEqualTo(Money.won(100_000));
        assertThat(result.estimatedRefundAt()).isEqualTo(Instant.parse("2026-05-26T00:00:00Z"));
        ArgumentCaptor<Return> returnCaptor = ArgumentCaptor.forClass(Return.class);
        verify(returnRepository).save(returnCaptor.capture(), eq("idem-1"));
        assertThat(returnCaptor.getValue().orderId()).isEqualTo("order-1");
        ArgumentCaptor<ReturnRequestedEvent> eventCaptor = ArgumentCaptor.forClass(ReturnRequestedEvent.class);
        verify(outboxPort).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().returnId()).isEqualTo(returnCaptor.getValue().id());
    }

    @Test
    @DisplayName("동일 idempotency key로 재요청하면 기존 Return 결과를 반환하고 이벤트를 중복 저장하지 않는다")
    void request_return_with_same_idempotency_key_returns_existing_result_without_duplicate_event() {
        Return existingReturn = Return.request(
            "return-1",
            "order-1",
            "user-1",
            ReturnReason.DEFECT,
            "broken",
            NOW
        );
        when(returnRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existingReturn));
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(deliveredOrder()));
        ReturnUseCase useCase = useCase();

        ReturnResult result = useCase.requestReturn(command("idem-1"));

        assertThat(result.returnId()).isEqualTo("return-1");
        assertThat(result.status()).isEqualTo(ReturnStatus.REQUESTED);
        verify(returnRepository, never()).save(any(Return.class), any());
        verify(outboxPort, never()).save(any());
    }

    @Test
    @DisplayName("반품 가능 상태가 아닌 주문은 반품 신청을 거절한다")
    void request_return_fails_when_order_is_not_returnable() {
        when(returnRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(shippingOrder()));
        ReturnUseCase useCase = useCase();

        assertThatThrownBy(() -> useCase.requestReturn(command("idem-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not returnable");
        verify(returnRepository, never()).save(any(Return.class), any());
        verify(outboxPort, never()).save(any());
    }

    @Test
    @DisplayName("저장 중 idempotency unique 충돌이 발생하면 기존 Return을 재조회해 반환한다")
    void request_return_returns_existing_result_when_idempotency_conflict_occurs() {
        Return existingReturn = Return.request(
            "return-1",
            "order-1",
            "user-1",
            ReturnReason.DEFECT,
            "broken",
            NOW
        );
        when(returnRepository.findByIdempotencyKey("idem-1"))
            .thenReturn(Optional.empty(), Optional.of(existingReturn));
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(deliveredOrder()));
        doThrow(new DuplicateReturnRequestException("idem-1", new RuntimeException("duplicate")))
            .when(returnRepository)
            .save(any(Return.class), eq("idem-1"));
        ReturnUseCase useCase = useCase();

        ReturnResult result = useCase.requestReturn(command("idem-1"));

        assertThat(result.returnId()).isEqualTo("return-1");
        assertThat(result.status()).isEqualTo(ReturnStatus.REQUESTED);
        verify(outboxPort, never()).save(any());
    }

    @Test
    @DisplayName("주문을 찾지 못하면 반품 신청을 실패 처리한다")
    void request_return_fails_when_order_does_not_exist() {
        when(returnRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.empty());
        ReturnUseCase useCase = useCase();

        assertThatThrownBy(() -> useCase.requestReturn(command("idem-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order not found");
        verify(returnRepository, never()).save(any(Return.class), any());
        verify(outboxPort, never()).save(any());
    }

    private ReturnUseCase useCase() {
        return new ReturnService(
            orderRepository,
            returnRepository,
            outboxPort,
            new ReturnPolicy(),
            CLOCK
        );
    }

    private ReturnCommand command(String idempotencyKey) {
        return new ReturnCommand("order-1", "user-1", ReturnReason.DEFECT, "broken", idempotencyKey);
    }

    private Order deliveredOrder() {
        return new Order(
            "order-1",
            "user-1",
            OrderStatus.DELIVERED,
            PaymentStatus.PAID,
            ShipmentStatus.DELIVERED,
            List.of(new OrderItem("product-1", "Sneakers", 2, Money.won(50_000))),
            Instant.parse("2026-05-19T00:00:00Z"),
            Instant.parse("2026-05-20T00:00:00Z")
        );
    }

    private Order shippingOrder() {
        return new Order(
            "order-1",
            "user-1",
            OrderStatus.SHIPPED,
            PaymentStatus.PAID,
            ShipmentStatus.IN_TRANSIT,
            List.of(new OrderItem("product-1", "Sneakers", 2, Money.won(50_000))),
            Instant.parse("2026-05-19T00:00:00Z"),
            null
        );
    }
}
