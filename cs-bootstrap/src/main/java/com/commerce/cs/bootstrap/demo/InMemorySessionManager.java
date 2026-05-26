package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.chat.ChatMessage;
import com.commerce.cs.application.chat.PendingAction;
import com.commerce.cs.application.chat.SessionContext;
import com.commerce.cs.application.chat.SessionManager;
import com.commerce.cs.application.session.SessionState;
import com.commerce.cs.application.session.SessionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"demo", "local"})
public class InMemorySessionManager implements SessionManager, SessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final int MAX_HISTORY_SIZE = 20;

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> histories = new ConcurrentHashMap<>();

    @Override
    public SessionContext loadContext(String sessionId) {
        SessionState sessionState = sessions.get(sessionId);
        ChatContext chatContext = new ChatContext(
            sessionId,
            sessionState == null ? null : sessionState.userId(),
            sessionState != null && sessionState.authenticated()
        );
        return new SessionContext(chatContext, pendingActions.get(sessionId), history(sessionId));
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
    public void appendUserMessage(String sessionId, String message) {
        appendMessage(sessionId, new ChatMessage(ChatMessage.Role.USER, message));
    }

    @Override
    public void appendAssistantMessage(String sessionId, String message) {
        appendMessage(sessionId, new ChatMessage(ChatMessage.Role.ASSISTANT, message));
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
        return history(sessionId).stream()
            .filter(message -> message.role() == ChatMessage.Role.ASSISTANT)
            .reduce((first, second) -> second)
            .map(ChatMessage::content);
    }

    public List<ChatMessage> history(String sessionId) {
        return List.copyOf(histories.getOrDefault(sessionId, List.of()));
    }

    public void save(SessionState sessionState) {
        save(sessionState, DEFAULT_TTL);
    }

    private void appendMessage(String sessionId, ChatMessage message) {
        histories.compute(sessionId, (ignored, current) -> {
            List<ChatMessage> next = new ArrayList<>(current == null ? List.of() : current);
            next.add(message);
            if (next.size() <= MAX_HISTORY_SIZE) {
                return List.copyOf(next);
            }
            return List.copyOf(next.subList(next.size() - MAX_HISTORY_SIZE, next.size()));
        });
    }
}
