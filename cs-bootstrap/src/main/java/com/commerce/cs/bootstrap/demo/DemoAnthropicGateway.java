package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.infra.llm.client.AnthropicGateway;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("demo")
public class DemoAnthropicGateway implements AnthropicGateway {

    @Override
    public AnthropicMessageResponse createMessage(LlmRequest request) {
        String message = request.userMessage() == null ? "" : request.userMessage().toLowerCase();
        if (message.contains("return") || message.contains("policy")) {
            return new AnthropicMessageResponse(List.of(
                new AnthropicMessageResponse.ContentBlock.ToolUse("search_faq", Map.of("query", request.userMessage()))
            ));
        }
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.Text("Demo assistant response: " + request.userMessage())
        ));
    }
}
