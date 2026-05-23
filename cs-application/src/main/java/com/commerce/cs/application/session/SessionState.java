package com.commerce.cs.application.session;

public final class SessionState {

    private final String sessionId;
    private final String userId;
    private final boolean authenticated;

    public SessionState(String sessionId, String userId, boolean authenticated) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        this.sessionId = sessionId;
        this.userId = userId;
        this.authenticated = authenticated;
    }

    public String sessionId() {
        return sessionId;
    }

    public String userId() {
        return userId;
    }

    public boolean authenticated() {
        return authenticated;
    }
}
