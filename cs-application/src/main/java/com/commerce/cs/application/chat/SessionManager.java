package com.commerce.cs.application.chat;

public interface SessionManager {

    SessionContext loadContext(String sessionId);

    void savePendingAction(String sessionId, PendingAction pendingAction);

    void clearPendingAction(String sessionId);

    void appendAssistantMessage(String sessionId, String message);
}
