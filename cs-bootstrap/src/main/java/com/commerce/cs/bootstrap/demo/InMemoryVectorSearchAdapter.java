package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.rag.SearchDocument;
import com.commerce.cs.application.rag.VectorSearchPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InMemoryVectorSearchAdapter implements VectorSearchPort {

    @Override
    public List<SearchDocument> search(String collection, String query, int topK) {
        SearchDocument document = new SearchDocument(
            collection + "-demo-1",
            "Demo " + collection + " result",
            "Demo search result for query: " + query,
            0.91,
            Map.of("collection", collection)
        );
        return List.of(document);
    }
}
