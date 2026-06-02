package com.commerce.cs.infra.commerce.rag;

import java.time.Duration;

public enum RagCollection {
    FAQ("faq", Duration.ofHours(1), 5),
    POLICY("policy", Duration.ofHours(24), 5),
    PRODUCT("product", Duration.ofMinutes(5), 5);

    private final String collectionName;
    private final Duration cacheTtl;
    private final int topK;

    RagCollection(String collectionName, Duration cacheTtl, int topK) {
        this.collectionName = collectionName;
        this.cacheTtl = cacheTtl;
        this.topK = topK;
    }

    public String collectionName() {
        return collectionName;
    }

    public Duration cacheTtl() {
        return cacheTtl;
    }

    public int topK() {
        return topK;
    }
}
