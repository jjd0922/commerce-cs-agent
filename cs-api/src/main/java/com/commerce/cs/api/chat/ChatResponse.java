package com.commerce.cs.api.chat;

import java.util.Map;

public record ChatResponse(
    String type,
    String message,
    Map<String, Object> data
) {

    public static ChatResponse text(String message) {
        return new ChatResponse("TEXT", message, Map.of());
    }

    public static ChatResponse requiresAuthentication(String message) {
        return new ChatResponse("REQUIRES_AUTHENTICATION", message, Map.of());
    }

    public static ChatResponse requiresConfirmation(String message) {
        return new ChatResponse("REQUIRES_CONFIRMATION", message, Map.of());
    }

    public static ChatResponse toolExecuted(Map<String, Object> data) {
        return new ChatResponse("TOOL_EXECUTED", null, data == null ? Map.of() : Map.copyOf(data));
    }

    public static ChatResponse cancelled(String message) {
        return new ChatResponse("CANCELLED", message, Map.of());
    }

    public static ChatResponse escalated(String reason) {
        return new ChatResponse("ESCALATED", reason, Map.of());
    }
}
