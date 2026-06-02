package com.commerce.cs.application.rag;

import java.util.Map;

public record SearchDocument(
    String id,
    String title,
    String content,
    double score,
    Map<String, Object> metadata
) {

    public SearchDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
