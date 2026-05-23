package com.commerce.cs.infra.persistence.returns;

import com.commerce.cs.domain.returns.Return;
import com.commerce.cs.domain.returns.ReturnReason;
import com.commerce.cs.domain.returns.ReturnStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "returns")
public class ReturnEntity {

    @Id
    private String id;

    private String orderId;
    private String userId;

    @Enumerated(EnumType.STRING)
    private ReturnReason reason;

    @Column(length = 1000)
    private String detail;

    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    protected ReturnEntity() {
    }

    public static ReturnEntity from(Return returnRequest, String idempotencyKey) {
        ReturnEntity entity = new ReturnEntity();
        entity.id = returnRequest.id();
        entity.orderId = returnRequest.orderId();
        entity.userId = returnRequest.userId();
        entity.reason = returnRequest.reason();
        entity.detail = returnRequest.detail();
        entity.requestedAt = returnRequest.requestedAt();
        entity.status = returnRequest.status();
        entity.idempotencyKey = idempotencyKey;
        return entity;
    }

    public Return toDomain() {
        return Return.restore(
            id,
            orderId,
            userId,
            reason,
            detail,
            requestedAt,
            status
        );
    }
}
