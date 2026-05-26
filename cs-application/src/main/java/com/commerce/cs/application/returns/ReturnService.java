package com.commerce.cs.application.returns;

import com.commerce.cs.application.order.OrderRepository;
import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.domain.event.ReturnRequestedEvent;
import com.commerce.cs.domain.order.Order;
import com.commerce.cs.domain.returns.Return;
import com.commerce.cs.domain.returns.ReturnPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReturnService implements ReturnUseCase {

    private final OrderRepository orderRepository;
    private final ReturnRepository returnRepository;
    private final OutboxPort outboxPort;
    private final ReturnPolicy returnPolicy;
    private final Clock clock;

    @Override
    @Transactional
    public ReturnResult requestReturn(ReturnCommand command) {
        return returnRepository.findByIdempotencyKey(command.idempotencyKey())
            .map(existing -> toResult(existing, existing.requestedAt(), order(command)))
            .orElseGet(() -> createReturn(command));
    }

    private ReturnResult createReturn(ReturnCommand command) {
        Instant now = clock.instant();
        Order order = order(command);
        returnPolicy.validate(order, command.reason(), now);

        Return returnRequest = Return.request(
            UUID.randomUUID().toString(),
            command.orderId(),
            command.userId(),
            command.reason(),
            command.detail(),
            now
        );
        returnRepository.save(returnRequest, command.idempotencyKey());
        outboxPort.save(new ReturnRequestedEvent(
            returnRequest.id(),
            returnRequest.orderId(),
            returnRequest.userId(),
            now
        ));
        return toResult(returnRequest, now, order);
    }

    private ReturnResult toResult(Return returnRequest, Instant requestedAt, Order order) {
        return new ReturnResult(
            returnRequest.id(),
            returnRequest.status(),
            returnPolicy.refundAmount(order),
            returnPolicy.estimateRefundAt(requestedAt)
        );
    }

    private Order order(ReturnCommand command) {
        return orderRepository.findByIdAndUserId(command.orderId(), command.userId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
