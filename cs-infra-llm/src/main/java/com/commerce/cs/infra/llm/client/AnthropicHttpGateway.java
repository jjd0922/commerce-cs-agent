package com.commerce.cs.infra.llm.client;

import com.commerce.cs.application.chat.ChatMessage;
import com.commerce.cs.application.llm.LlmRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Primary
@Component
@Profile("live-llm")
public class AnthropicHttpGateway implements AnthropicGateway {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public AnthropicHttpGateway(
        ObjectMapper objectMapper,
        @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
        @Value("${anthropic.api-key:}") String apiKey,
        @Value("${anthropic.model:claude-haiku-4-5-20251001}") String model,
        @Value("${anthropic.max-tokens:128}") int maxTokens,
        @Value("${anthropic.temperature:0.0}") double temperature
    ) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public AnthropicMessageResponse createMessage(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmClientException("ANTHROPIC_API_KEY is required for live LLM evaluation");
        }

        JsonNode response = restClient.post()
            .uri("/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .body(body(request))
            .retrieve()
            .body(JsonNode.class);

        return parse(response);
    }

    private Map<String, Object> body(LlmRequest request) {
        return Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "temperature", temperature,
            "system", systemPrompt(),
            "tools", tools(),
            "messages", messages(request)
        );
    }

    private String systemPrompt() {
        return """
            You are a commerce customer-support tool router.
            Choose exactly one tool when the user intent clearly matches a tool.
            Return plain text only when the request is ambiguous or no tool is appropriate.
            For return requests, use request_return with orderId, reason, and detail; backend authentication and confirmation gates will control execution.
            Do not invent policy decisions outside the tools.
            """;
    }

    private List<Map<String, Object>> messages(LlmRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage message : request.history()) {
            messages.add(Map.of(
                "role", role(message.role()),
                "content", message.content()
            ));
        }
        messages.add(Map.of(
            "role", "user",
            "content", request.userMessage()
        ));
        return messages;
    }

    private String role(ChatMessage.Role role) {
        return role == ChatMessage.Role.ASSISTANT ? "assistant" : "user";
    }

    private List<Map<String, Object>> tools() {
        return List.of(
            tool("search_faq", "Search FAQ documents for general customer-support questions.", querySchema()),
            tool("get_policy", "Search policy documents for return, exchange, and shipping rules.", querySchema()),
            tool("search_product", "Search product documents for product questions.", querySchema()),
            tool("request_return", "Request a return for an authenticated customer's order.", returnSchema())
        );
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
            "name", name,
            "description", description,
            "input_schema", inputSchema
        );
    }

    private Map<String, Object> querySchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "The user's search query.")
            ),
            "required", List.of("query")
        );
    }

    private Map<String, Object> returnSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "orderId", Map.of("type", "string", "description", "The order id to return."),
                "reason", Map.of(
                    "type", "string",
                    "enum", List.of("DEFECT", "WRONG_ITEM", "CHANGED_MIND", "OTHER")
                ),
                "detail", Map.of("type", "string", "description", "Short return reason detail.")
            ),
            "required", List.of("orderId", "reason", "detail")
        );
    }

    private AnthropicMessageResponse parse(JsonNode response) {
        if (response == null || !response.has("content")) {
            throw new LlmResponseParseException("Anthropic response content is empty");
        }

        List<AnthropicMessageResponse.ContentBlock> blocks = new ArrayList<>();
        for (JsonNode block : response.get("content")) {
            String type = block.path("type").asText();
            if ("text".equals(type)) {
                blocks.add(new AnthropicMessageResponse.ContentBlock.Text(block.path("text").asText()));
            }
            if ("tool_use".equals(type)) {
                blocks.add(new AnthropicMessageResponse.ContentBlock.ToolUse(
                    block.path("name").asText(),
                    objectMapper.convertValue(block.path("input"), MAP_TYPE)
                ));
            }
        }
        return new AnthropicMessageResponse(blocks);
    }
}
