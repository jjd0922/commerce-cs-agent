package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.chat.PendingAction;
import com.commerce.cs.application.chat.SessionContext;
import com.commerce.cs.application.chat.SessionManager;
import com.commerce.cs.application.session.SessionState;
import com.commerce.cs.application.session.SessionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("demo")
public class InMemorySessionManager implements SessionManager, SessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<String, String> assistantMessages = new ConcurrentHashMap<>();

    @Override
    public SessionContext loadContext(String sessionId) {
        SessionState sessionState = sessions.get(sessionId);
        ChatContext chatContext = new ChatContext(
            sessionId,
            sessionState == null ? null : sessionState.userId(),
            sessionState != null && sessionState.authenticated()
        );
        return new SessionContext(chatContext, pendingActions.get(sessionId));
    }

    @Override
    public void savePendingAction(String sessionId, PendingAction pendingAction) {
        pendingActions.put(sessionId, pendingAction);
    }

    @Override
    public void clearPendingAction(String sessionId) {
        pendingActions.remove(sessionId);
    }

    @Override
    public void appendAssistantMessage(String sessionId, String message) {
        assistantMessages.put(sessionId, message);
    }

    @Override
    public Optional<SessionState> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(SessionState sessionState, Duration ttl) {
        sessions.put(sessionState.sessionId(), sessionState);
    }

    public Optional<String> lastAssistantMessage(String sessionId) {
        return Optional.ofNullable(assistantMessages.get(sessionId));
    }

    public void save(SessionState sessionState) {
        save(sessionState, DEFAULT_TTL);
    }
}
