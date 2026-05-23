package com.commerce.cs.application.returns;

import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.returns.ReturnStatus;

import java.time.Instant;

public record ReturnResult(
    String returnId,
    ReturnStatus status,
    Money refundAmount,
    Instant estimatedRefundAt
) {
}
