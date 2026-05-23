package com.commerce.cs.application.llm;

import com.commerce.cs.application.chat.ChatContext;

public final class LlmRequest {

    private final ChatContext context;
    private final String userMessage;

    public LlmRequest(ChatContext context, String userMessage) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        this.context = context;
        this.userMessage = userMessage;
    }

    public ChatContext context() {
        return context;
    }

    public String userMessage() {
        return userMessage;
    }
}
