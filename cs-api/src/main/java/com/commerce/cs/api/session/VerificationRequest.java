package com.commerce.cs.api.session;

public record VerificationRequest(
    String sessionId,
    String email,
    String phoneLast4
) {
}
