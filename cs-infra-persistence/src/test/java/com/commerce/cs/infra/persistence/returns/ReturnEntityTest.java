package com.commerce.cs.infra.persistence.returns;

import com.commerce.cs.domain.returns.Return;
import com.commerce.cs.domain.returns.ReturnReason;
import com.commerce.cs.domain.returns.ReturnStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReturnEntity 반품 도메인 변환")
class ReturnEntityTest {

    @Test
    @DisplayName("반품 엔티티를 반품 도메인으로 변환한다")
    void maps_return_entity_to_domain() {
        Return returnRequest = Return.request(
            "return-1",
            "order-1",
            "user-1",
            ReturnReason.DEFECT,
            "broken",
            Instant.parse("2026-05-23T00:00:00Z")
        );

        Return mapped = ReturnEntity.from(returnRequest, "idem-1").toDomain();

        assertThat(mapped.id()).isEqualTo("return-1");
        assertThat(mapped.orderId()).isEqualTo("order-1");
        assertThat(mapped.userId()).isEqualTo("user-1");
        assertThat(mapped.reason()).isEqualTo(ReturnReason.DEFECT);
        assertThat(mapped.status()).isEqualTo(ReturnStatus.REQUESTED);
    }
}
