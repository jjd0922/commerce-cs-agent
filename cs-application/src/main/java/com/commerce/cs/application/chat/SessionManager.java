package com.commerce.cs.application.chat;

public interface SessionManager {

    SessionContext loadContext(String sessionId);

    void savePendingAction(String sessionId, PendingAction pendingAction);

    void clearPendingAction(String sessionId);

    void appendUserMessage(String sessionId, String message);

    void appendAssistantMessage(String sessionId, String message);
}
