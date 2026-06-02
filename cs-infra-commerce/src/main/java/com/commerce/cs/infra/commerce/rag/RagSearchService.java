package com.commerce.cs.infra.commerce.rag;

import com.commerce.cs.application.rag.QueryResultCachePort;
import com.commerce.cs.application.rag.SearchDocument;
import com.commerce.cs.application.rag.VectorSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagSearchService {

    private final VectorSearchPort vectorSearchPort;
    private final QueryResultCachePort queryResultCachePort;

    public List<SearchDocument> search(RagCollection collection, String query) {
        String normalizedQuery = QueryNormalizer.normalize(query);
        String cacheKey = collection.collectionName() + ":" + normalizedQuery;
        return queryResultCachePort.get(cacheKey)
            .orElseGet(() -> searchAndCache(collection, query, cacheKey));
    }

    private List<SearchDocument> searchAndCache(RagCollection collection, String query, String cacheKey) {
        List<SearchDocument> documents = vectorSearchPort.search(
            collection.collectionName(),
            query,
            collection.topK()
        );
        queryResultCachePort.put(cacheKey, documents, collection.cacheTtl());
        return documents;
    }
}
