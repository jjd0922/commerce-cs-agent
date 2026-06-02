package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.infra.llm.client.AnthropicGateway;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Profile({"demo", "local"})
public class DemoAnthropicGateway implements AnthropicGateway {

    @Override
    public AnthropicMessageResponse createMessage(LlmRequest request) {
        String userMessage = request.userMessage() == null ? "" : request.userMessage();
        String message = userMessage.toLowerCase(Locale.ROOT);
        if (message.contains("fallback")) {
            return text("The automated response is temporarily unavailable. Please try again later or request a human agent.");
        }
        if (message.contains("faq")) {
            return tool("search_faq", Map.of("query", userMessage));
        }
        if (message.contains("policy") && !message.contains("order-2")) {
            return tool("get_policy", Map.of("query", userMessage));
        }
        if (message.contains("product")) {
            return tool("search_product", Map.of("query", userMessage));
        }
        if (message.contains("return")) {
            return tool("request_return", Map.of(
                "orderId", message.contains("order-2") ? "order-2" : "order-1",
                "reason", "DEFECT",
                "detail", "broken item"
            ));
        }
        return text("Demo assistant response: " + userMessage);
    }

    private AnthropicMessageResponse tool(String toolName, Map<String, Object> args) {
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.ToolUse(toolName, args)
        ));
    }

    private AnthropicMessageResponse text(String text) {
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.Text(text)
        ));
    }
}
