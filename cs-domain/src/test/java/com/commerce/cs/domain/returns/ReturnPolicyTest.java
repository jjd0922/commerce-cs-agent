package com.commerce.cs.domain.returns;

import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.order.OrderItem;
import com.commerce.cs.domain.order.OrderStatus;
import com.commerce.cs.domain.order.PaymentStatus;
import com.commerce.cs.domain.order.ShipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("반품 정책")
class ReturnPolicyTest {

    private final ReturnPolicy policy = new ReturnPolicy();

    @Test
    @DisplayName("배송 완료 및 결제 완료 주문은 반품 가능 기간 안에 반품할 수 있다")
    void delivered_paid_order_is_returnable_within_return_window() {
        // given
        Order order = deliveredOrder(Instant.parse("2026-05-20T00:00:00Z"));

        // when
        boolean returnable = policy.isReturnable(order, Instant.parse("2026-05-23T00:00:00Z"));

        // then
        assertThat(returnable).isTrue();
    }

    @Test
    @DisplayName("반품 가능 기간이 지나면 주문을 반품할 수 없다")
    void order_is_not_returnable_after_return_window() {
        // given
        Order order = deliveredOrder(Instant.parse("2026-05-01T00:00:00Z"));

        // when
        boolean returnable = policy.isReturnable(order, Instant.parse("2026-05-23T00:00:00Z"));

        // then
        assertThat(returnable).isFalse();
    }

    @Test
    @DisplayName("아직 배송 완료되지 않은 주문은 반품할 수 없다")
    void not_delivered_order_is_not_returnable() {
        // given
        Order order = new Order(
            "order-1",
            "user-1",
            OrderStatus.SHIPPED,
            PaymentStatus.PAID,
            ShipmentStatus.IN_TRANSIT,
            List.of(new OrderItem("product-1", "Sneakers", 1, Money.won(50_000))),
            Instant.parse("2026-05-20T00:00:00Z"),
            null
        );

        // when & then
        assertThatThrownBy(() -> policy.validate(order, ReturnReason.CHANGED_MIND, Instant.parse("2026-05-23T00:00:00Z")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not returnable");
    }

    @Test
    @DisplayName("하자와 오배송 사유는 증빙 자료가 필요하다")
    void defect_and_wrong_item_require_evidence() {
        // when & then
        assertThat(policy.reasonRequiresEvidence(ReturnReason.DEFECT)).isTrue();
        assertThat(policy.reasonRequiresEvidence(ReturnReason.WRONG_ITEM)).isTrue();
        assertThat(policy.reasonRequiresEvidence(ReturnReason.CHANGED_MIND)).isFalse();
    }

    @Test
    @DisplayName("환불 금액은 주문 총액으로 계산한다")
    void refund_amount_is_order_total_amount() {
        // given
        Order order = deliveredOrder(Instant.parse("2026-05-20T00:00:00Z"));

        // when & then
        assertThat(policy.refundAmount(order)).isEqualTo(Money.won(100_000));
    }

    private Order deliveredOrder(Instant deliveredAt) {
        return new Order(
            "order-1",
            "user-1",
            OrderStatus.DELIVERED,
            PaymentStatus.PAID,
            ShipmentStatus.DELIVERED,
            List.of(new OrderItem("product-1", "Sneakers", 2, Money.won(50_000))),
            Instant.parse("2026-05-19T00:00:00Z"),
            deliveredAt
        );
    }
}
