package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Entry> values = new ConcurrentHashMap<>();

    @Override
    public Optional<ToolResult> get(String key) {
        Entry entry = values.get(key);
        if (entry == null || entry.expired()) {
            values.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result());
    }

    @Override
    public void save(String key, ToolResult result, Duration ttl) {
        values.put(key, new Entry(result, Instant.now().plus(ttl)));
    }

    private record Entry(ToolResult result, Instant expiresAt) {

        private boolean expired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
