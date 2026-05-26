package com.commerce.cs.application.llm;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.chat.ChatMessage;

import java.util.List;

public final class LlmRequest {

    private final ChatContext context;
    private final String userMessage;
    private final List<ChatMessage> history;

    public LlmRequest(ChatContext context, String userMessage) {
        this(context, userMessage, List.of());
    }

    public LlmRequest(ChatContext context, String userMessage, List<ChatMessage> history) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        this.context = context;
        this.userMessage = userMessage;
        this.history = history == null ? List.of() : List.copyOf(history);
    }

    public ChatContext context() {
        return context;
    }

    public String userMessage() {
        return userMessage;
    }

    public List<ChatMessage> history() {
        return history;
    }
}
