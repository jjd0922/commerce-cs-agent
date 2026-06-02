package com.commerce.cs.application.chat;

public final class ChatContext {

    private final String sessionId;
    private final String userId;
    private final boolean authenticated;
    private final String currentIdempotencyKey;

    public ChatContext(String sessionId, String userId, boolean authenticated) {
        this(sessionId, userId, authenticated, null);
    }

    private ChatContext(String sessionId, String userId, boolean authenticated, String currentIdempotencyKey) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        this.sessionId = sessionId;
        this.userId = userId;
        this.authenticated = authenticated;
        this.currentIdempotencyKey = currentIdempotencyKey;
    }

    public ChatContext withCurrentIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        return new ChatContext(sessionId, userId, authenticated, idempotencyKey);
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

    public String currentIdempotencyKey() {
        return currentIdempotencyKey;
    }
}
