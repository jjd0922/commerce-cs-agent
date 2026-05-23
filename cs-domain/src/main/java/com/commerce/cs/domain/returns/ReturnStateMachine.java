package com.commerce.cs.domain.returns;

import java.util.Map;
import java.util.Set;

import static com.commerce.cs.domain.returns.ReturnStatus.APPROVED;
import static com.commerce.cs.domain.returns.ReturnStatus.CANCELLED;
import static com.commerce.cs.domain.returns.ReturnStatus.COMPLETED;
import static com.commerce.cs.domain.returns.ReturnStatus.INSPECTING;
import static com.commerce.cs.domain.returns.ReturnStatus.PICKED_UP;
import static com.commerce.cs.domain.returns.ReturnStatus.PICKUP_SCHEDULED;
import static com.commerce.cs.domain.returns.ReturnStatus.REFUND_ISSUED;
import static com.commerce.cs.domain.returns.ReturnStatus.REJECTED;
import static com.commerce.cs.domain.returns.ReturnStatus.REQUESTED;

public final class ReturnStateMachine {

    private static final Map<ReturnStatus, Set<ReturnStatus>> TRANSITIONS = Map.of(
        REQUESTED, Set.of(APPROVED, REJECTED, CANCELLED),
        APPROVED, Set.of(PICKUP_SCHEDULED, CANCELLED),
        PICKUP_SCHEDULED, Set.of(PICKED_UP, CANCELLED),
        PICKED_UP, Set.of(INSPECTING),
        INSPECTING, Set.of(COMPLETED, REJECTED),
        COMPLETED, Set.of(REFUND_ISSUED),
        REFUND_ISSUED, Set.of(),
        REJECTED, Set.of(),
        CANCELLED, Set.of()
    );

    private ReturnStateMachine() {
    }

    public static boolean canTransition(ReturnStatus from, ReturnStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransition(ReturnStatus from, ReturnStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalArgumentException("Invalid return state transition: " + from + " -> " + to);
        }
    }
}
