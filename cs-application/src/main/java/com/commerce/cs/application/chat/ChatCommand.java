package com.commerce.cs.application.chat;

public final class ChatCommand {

    private final String sessionId;
    private final String message;

    public ChatCommand(String sessionId, String message) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        this.sessionId = sessionId;
        this.message = message;
    }

    public String sessionId() {
        return sessionId;
    }

    public String message() {
        return message;
    }
}
