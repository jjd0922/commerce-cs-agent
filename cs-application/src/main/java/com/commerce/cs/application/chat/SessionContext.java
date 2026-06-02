package com.commerce.cs.application.chat;

public final class SessionContext {

    private final ChatContext chatContext;
    private final PendingAction pendingAction;

    public SessionContext(ChatContext chatContext, PendingAction pendingAction) {
        if (chatContext == null) {
            throw new IllegalArgumentException("chatContext must not be null");
        }
        this.chatContext = chatContext;
        this.pendingAction = pendingAction;
    }

    public ChatContext chatContext() {
        return chatContext;
    }

    public PendingAction pendingAction() {
        return pendingAction;
    }

    public boolean hasPendingAction() {
        return pendingAction != null;
    }
}
