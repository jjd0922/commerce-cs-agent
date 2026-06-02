package com.commerce.cs.application.chat;

public record ChatMessage(
    Role role,
    String content
) {

    public ChatMessage {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    public enum Role {
        USER,
        ASSISTANT
    }
}
