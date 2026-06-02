package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.rag.QueryResultCachePort;
import com.commerce.cs.application.rag.SearchDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"demo", "local"})
public class InMemoryQueryResultCacheAdapter implements QueryResultCachePort {

    private final Map<String, Entry> values = new ConcurrentHashMap<>();

    @Override
    public Optional<List<SearchDocument>> get(String key) {
        Entry entry = values.get(key);
        if (entry == null || entry.expired()) {
            values.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.documents());
    }

    @Override
    public void put(String key, List<SearchDocument> documents, Duration ttl) {
        values.put(key, new Entry(List.copyOf(documents), Instant.now().plus(ttl)));
    }

    private record Entry(List<SearchDocument> documents, Instant expiresAt) {

        private boolean expired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
