package com.commerce.cs.infra.commerce.rag;

import java.text.Normalizer;
import java.util.Locale;

public final class QueryNormalizer {

    private QueryNormalizer() {
    }

    public static String normalize(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(query, Normalizer.Form.NFKC)
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
        return normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit} ]", "");
    }
}
