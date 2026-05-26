package com.commerce.cs.infra.commerce.rag;

import com.commerce.cs.application.rag.QueryResultCachePort;
import com.commerce.cs.application.rag.SearchDocument;
import com.commerce.cs.application.rag.VectorSearchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RAG 검색 서비스")
@ExtendWith(MockitoExtension.class)
class RagSearchServiceTest {

    @Mock
    private VectorSearchPort vectorSearchPort;

    @Mock
    private QueryResultCachePort cachePort;

    @Test
    @DisplayName("캐시가 없으면 벡터 검색 결과를 조회하고 캐시에 저장한다")
    void searches_vector_store_and_caches_result_when_cache_misses() {
        List<SearchDocument> expected = List.of(document("faq-1"));
        when(cachePort.get("faq:return policy")).thenReturn(Optional.empty());
        when(vectorSearchPort.search("faq", "Return Policy", 5)).thenReturn(expected);
        RagSearchService service = new RagSearchService(vectorSearchPort, cachePort);

        List<SearchDocument> documents = service.search(RagCollection.FAQ, "Return Policy");

        assertThat(documents).isSameAs(expected);
        verify(vectorSearchPort).search("faq", "Return Policy", 5);
        verify(cachePort).put("faq:return policy", expected, Duration.ofHours(1));
    }

    @Test
    @DisplayName("캐시가 있으면 벡터 검색을 호출하지 않고 캐시 결과를 반환한다")
    void returns_cached_result_when_cache_hits() {
        List<SearchDocument> cached = List.of(document("cached"));
        when(cachePort.get("faq:return policy")).thenReturn(Optional.of(cached));
        RagSearchService service = new RagSearchService(vectorSearchPort, cachePort);

        List<SearchDocument> documents = service.search(RagCollection.FAQ, "Return Policy");

        assertThat(documents).isSameAs(cached);
        assertThat(documents).extracting(SearchDocument::id).containsExactly("cached");
        verify(vectorSearchPort, never()).search(eq("faq"), eq("Return Policy"), eq(5));
    }

    private static SearchDocument document(String id) {
        return new SearchDocument(id, "title", "content", 0.9, Map.of());
    }
}
