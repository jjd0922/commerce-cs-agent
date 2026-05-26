package com.commerce.cs.infra.commerce.rag.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.rag.SearchDocument;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.infra.commerce.rag.RagCollection;
import com.commerce.cs.infra.commerce.rag.RagSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RAG 조회 Tool")
@ExtendWith(MockitoExtension.class)
class RagToolTest {

    @Mock
    private RagSearchService ragSearchService;

    @Test
    @DisplayName("FAQ 검색 Tool은 인증과 confirmation 없이 FAQ 결과를 반환한다")
    void search_faq_tool_returns_answers() {
        when(ragSearchService.search(RagCollection.FAQ, "return policy")).thenReturn(documents("faq-1"));
        SearchFaqTool tool = new SearchFaqTool(ragSearchService);

        ToolResult result = tool.execute(Map.of("query", "return policy"), context());

        assertThat(tool.name()).isEqualTo("search_faq");
        assertThat(tool.requiresAuthentication()).isFalse();
        assertThat(tool.mutation()).isFalse();
        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsKey("answers");
        verify(ragSearchService).search(RagCollection.FAQ, "return policy");
    }

    @Test
    @DisplayName("정책 조회 Tool은 인증과 confirmation 없이 정책 결과를 반환한다")
    void get_policy_tool_returns_policies() {
        when(ragSearchService.search(RagCollection.POLICY, "exchange period")).thenReturn(documents("policy-1"));
        GetPolicyTool tool = new GetPolicyTool(ragSearchService);

        ToolResult result = tool.execute(Map.of("query", "exchange period"), context());

        assertThat(tool.name()).isEqualTo("get_policy");
        assertThat(tool.requiresAuthentication()).isFalse();
        assertThat(tool.mutation()).isFalse();
        assertThat(result.data()).containsKey("policies");
        verify(ragSearchService).search(RagCollection.POLICY, "exchange period");
    }

    @Test
    @DisplayName("상품 검색 Tool은 인증과 confirmation 없이 상품 결과를 반환한다")
    void search_product_tool_returns_products() {
        when(ragSearchService.search(RagCollection.PRODUCT, "sneakers")).thenReturn(documents("product-1"));
        SearchProductTool tool = new SearchProductTool(ragSearchService);

        ToolResult result = tool.execute(Map.of("query", "sneakers"), context());

        assertThat(tool.name()).isEqualTo("search_product");
        assertThat(tool.requiresAuthentication()).isFalse();
        assertThat(tool.mutation()).isFalse();
        assertThat(result.data()).containsKey("products");
        verify(ragSearchService).search(RagCollection.PRODUCT, "sneakers");
    }

    @Test
    @DisplayName("query 인자가 없으면 Tool 실행에 실패한다")
    void tool_fails_when_query_is_missing() {
        SearchFaqTool tool = new SearchFaqTool(ragSearchService);

        assertThatThrownBy(() -> tool.execute(Map.of(), context()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("query");
    }

    private ChatContext context() {
        return new ChatContext("session-1", null, false);
    }

    private List<SearchDocument> documents(String id) {
        return List.of(new SearchDocument(
            id,
            "title",
            "content",
            0.91,
            Map.of("collection", id)
        ));
    }
}
