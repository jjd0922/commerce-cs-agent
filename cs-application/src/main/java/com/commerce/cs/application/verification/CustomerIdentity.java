package com.commerce.cs.application.verification;

public final class CustomerIdentity {

    private final String userId;
    private final String email;
    private final String phoneLast4;

    public CustomerIdentity(String userId, String email, String phoneLast4) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (phoneLast4 == null || !phoneLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("phoneLast4 must be 4 digits");
        }
        this.userId = userId;
        this.email = email;
        this.phoneLast4 = phoneLast4;
    }

    public boolean matches(String email, String phoneLast4) {
        return this.email.equalsIgnoreCase(email) && this.phoneLast4.equals(phoneLast4);
    }

    public String userId() {
        return userId;
    }

    public String email() {
        return email;
    }

    public String phoneLast4() {
        return phoneLast4;
    }
}
