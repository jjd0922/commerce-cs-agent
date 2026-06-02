package com.commerce.cs.api.chat;

import com.commerce.cs.application.chat.ChatResult;

public final class ChatResponseMapper {

    private ChatResponseMapper() {
    }

    public static ChatResponse from(ChatResult result) {
        if (result instanceof ChatResult.Text text) {
            return ChatResponse.text(text.message());
        }
        if (result instanceof ChatResult.RequiresAuthentication requiresAuthentication) {
            return ChatResponse.requiresAuthentication(requiresAuthentication.message());
        }
        if (result instanceof ChatResult.RequiresConfirmation requiresConfirmation) {
            return ChatResponse.requiresConfirmation(requiresConfirmation.message());
        }
        if (result instanceof ChatResult.ToolExecuted toolExecuted) {
            return ChatResponse.toolExecuted(toolExecuted.toolResult().data());
        }
        if (result instanceof ChatResult.Cancelled cancelled) {
            return ChatResponse.cancelled(cancelled.message());
        }
        if (result instanceof ChatResult.Escalated escalated) {
            return ChatResponse.escalated(escalated.reason());
        }
        throw new IllegalArgumentException("Unsupported chat result: " + result.getClass().getName());
    }
}
