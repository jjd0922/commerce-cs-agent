package com.commerce.cs.application.rag;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface QueryResultCachePort {

    Optional<List<SearchDocument>> get(String key);

    void put(String key, List<SearchDocument> documents, Duration ttl);
}
