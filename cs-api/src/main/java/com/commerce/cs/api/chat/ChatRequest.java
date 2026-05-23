package com.commerce.cs.api.chat;

public record ChatRequest(
    String sessionId,
    String message
) {
}
