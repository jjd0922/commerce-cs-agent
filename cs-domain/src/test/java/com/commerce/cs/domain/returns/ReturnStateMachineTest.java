package com.commerce.cs.domain.returns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.commerce.cs.domain.returns.ReturnStatus.APPROVED;
import static com.commerce.cs.domain.returns.ReturnStatus.PICKUP_SCHEDULED;
import static com.commerce.cs.domain.returns.ReturnStatus.REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("반품 상태머신")
class ReturnStateMachineTest {

    @Test
    @DisplayName("요청 상태의 반품은 승인 상태로 전이할 수 있다")
    void requested_to_approved_is_allowed() {
        // when & then
        assertThat(ReturnStateMachine.canTransition(REQUESTED, APPROVED)).isTrue();
    }

    @Test
    @DisplayName("요청 상태의 반품은 수거 예약 상태로 직접 전이할 수 없다")
    void requested_to_pickup_scheduled_is_not_allowed() {
        // when
        assertThat(ReturnStateMachine.canTransition(REQUESTED, PICKUP_SCHEDULED)).isFalse();

        // then
        assertThatThrownBy(() -> ReturnStateMachine.assertTransition(REQUESTED, PICKUP_SCHEDULED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid return state transition");
    }

    @Test
    @DisplayName("반품 엔티티는 상태머신을 통해서만 상태를 변경한다")
    void return_entity_changes_status_through_state_machine() {
        // given
        Return requested = Return.request(
            "return-1",
            "order-1",
            "user-1",
            ReturnReason.DEFECT,
            "broken",
            java.time.Instant.parse("2026-05-23T00:00:00Z")
        );

        // when
        requested.transitionTo(APPROVED);
        requested.transitionTo(PICKUP_SCHEDULED);

        // then
        assertThat(requested.status()).isEqualTo(PICKUP_SCHEDULED);
    }
}
