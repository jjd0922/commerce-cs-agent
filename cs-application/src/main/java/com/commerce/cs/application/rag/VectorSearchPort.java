package com.commerce.cs.application.rag;

import java.util.List;

public interface VectorSearchPort {

    List<SearchDocument> search(String collection, String query, int topK);
}
