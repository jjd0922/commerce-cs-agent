package com.commerce.cs.infra.llm.parser;

import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import com.commerce.cs.infra.llm.client.LlmResponseParseException;
import org.springframework.stereotype.Component;

@Component
public class ResponseParser {

    public LlmResponse parse(AnthropicMessageResponse response) {
        if (response == null || response.content().isEmpty()) {
            throw new LlmResponseParseException("LLM response content is empty");
        }

        AnthropicMessageResponse.ContentBlock first = response.content().get(0);
        if (first instanceof AnthropicMessageResponse.ContentBlock.Text text) {
            return parseText(text);
        }
        if (first instanceof AnthropicMessageResponse.ContentBlock.ToolUse toolUse) {
            return parseToolUse(toolUse);
        }
        throw new LlmResponseParseException("Unsupported LLM content block: " + first.getClass().getName());
    }

    private LlmResponse parseText(AnthropicMessageResponse.ContentBlock.Text text) {
        if (text.text() == null || text.text().isBlank()) {
            throw new LlmResponseParseException("LLM text response is blank");
        }
        return new LlmResponse.Text(text.text());
    }

    private LlmResponse parseToolUse(AnthropicMessageResponse.ContentBlock.ToolUse toolUse) {
        if (toolUse.name() == null || toolUse.name().isBlank()) {
            throw new LlmResponseParseException("LLM tool_use name is blank");
        }
        return new LlmResponse.ToolUse(toolUse.name(), toolUse.input());
    }
}
