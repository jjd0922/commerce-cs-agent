package com.commerce.cs.application.session;

import java.time.Duration;
import java.util.Optional;

public interface SessionStore {

    Optional<SessionState> findById(String sessionId);

    void save(SessionState sessionState, Duration ttl);
}
