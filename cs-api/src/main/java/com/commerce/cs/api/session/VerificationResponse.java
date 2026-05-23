package com.commerce.cs.api.session;

public record VerificationResponse(
    boolean success,
    String userId,
    String reason
) {

    public static VerificationResponse success(String userId) {
        return new VerificationResponse(true, userId, null);
    }

    public static VerificationResponse failure(String reason) {
        return new VerificationResponse(false, null, reason);
    }
}
