package com.commerce.cs.application.verification;

public final class VerificationCommand {

    private final String sessionId;
    private final String email;
    private final String phoneLast4;

    public VerificationCommand(String sessionId, String email, String phoneLast4) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (phoneLast4 == null || !phoneLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("phoneLast4 must be 4 digits");
        }
        this.sessionId = sessionId;
        this.email = email;
        this.phoneLast4 = phoneLast4;
    }

    public String sessionId() {
        return sessionId;
    }

    public String email() {
        return email;
    }

    public String phoneLast4() {
        return phoneLast4;
    }
}
