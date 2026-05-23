package com.commerce.cs.application.chat;

import java.util.Locale;
import java.util.Set;

public final class ConfirmationDecider {

    private static final Set<String> APPROVALS = Set.of(
        "yes",
        "y",
        "ok",
        "confirm",
        "proceed",
        "\uC608",
        "\uB124",
        "\uD655\uC778",
        "\uC9C4\uD589"
    );
    private static final Set<String> REJECTIONS = Set.of(
        "no",
        "n",
        "cancel",
        "stop",
        "\uC544\uB2C8\uC624",
        "\uC544\uB2C8\uC694",
        "\uCDE8\uC18C",
        "\uC911\uB2E8"
    );

    private ConfirmationDecider() {
    }

    public static ConfirmationDecision decide(String message) {
        if (message == null) {
            return ConfirmationDecision.UNKNOWN;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (APPROVALS.contains(normalized)) {
            return ConfirmationDecision.APPROVE;
        }
        if (REJECTIONS.contains(normalized)) {
            return ConfirmationDecision.REJECT;
        }
        return ConfirmationDecision.UNKNOWN;
    }
}
