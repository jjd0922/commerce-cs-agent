package com.commerce.cs.infra.commerce.rag.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.rag.SearchDocument;
import com.commerce.cs.application.tool.ToolHandler;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.infra.commerce.rag.RagCollection;
import com.commerce.cs.infra.commerce.rag.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetPolicyTool implements ToolHandler {

    private final RagSearchService ragSearchService;

    @Override
    public String name() {
        return "get_policy";
    }

    @Override
    public String description() {
        return "Search policy documents for return, exchange, and shipping rules.";
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Override
    public boolean mutation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ChatContext context) {
        return ToolResult.success(Map.of("policies", toResponse(ragSearchService.search(RagCollection.POLICY, query(args)))));
    }

    private String query(Map<String, Object> args) {
        Object query = args.get("query");
        if (query == null || query.toString().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.toString();
    }

    private List<Map<String, Object>> toResponse(List<SearchDocument> documents) {
        return documents.stream()
            .map(document -> Map.<String, Object>of(
                "id", document.id(),
                "title", document.title(),
                "content", document.content(),
                "score", document.score(),
                "metadata", document.metadata()
            ))
            .toList();
    }
}
