package com.commerce.cs.application.chat;

import java.util.List;

public final class SessionContext {

    private final ChatContext chatContext;
    private final PendingAction pendingAction;
    private final List<ChatMessage> history;

    public SessionContext(ChatContext chatContext, PendingAction pendingAction) {
        this(chatContext, pendingAction, List.of());
    }

    public SessionContext(ChatContext chatContext, PendingAction pendingAction, List<ChatMessage> history) {
        if (chatContext == null) {
            throw new IllegalArgumentException("chatContext must not be null");
        }
        this.chatContext = chatContext;
        this.pendingAction = pendingAction;
        this.history = history == null ? List.of() : List.copyOf(history);
    }

    public ChatContext chatContext() {
        return chatContext;
    }

    public PendingAction pendingAction() {
        return pendingAction;
    }

    public List<ChatMessage> history() {
        return history;
    }

    public boolean hasPendingAction() {
        return pendingAction != null;
    }
}
