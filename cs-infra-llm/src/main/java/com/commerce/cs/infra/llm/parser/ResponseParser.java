package com.commerce.cs.infra.llm.parser;

import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import com.commerce.cs.infra.llm.client.LlmResponseParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResponseParser {

    public LlmResponse parse(AnthropicMessageResponse response) {
        if (response == null || response.content().isEmpty()) {
            throw new LlmResponseParseException("LLM response content is empty");
        }

        AnthropicMessageResponse.ContentBlock.ToolUse toolUse = null;
        List<String> texts = new ArrayList<>();

        for (AnthropicMessageResponse.ContentBlock block : response.content()) {
            if (block instanceof AnthropicMessageResponse.ContentBlock.ToolUse currentToolUse) {
                if (toolUse != null) {
                    throw new LlmResponseParseException("Multiple LLM tool_use blocks are not supported");
                }
                toolUse = currentToolUse;
                continue;
            }
            if (block instanceof AnthropicMessageResponse.ContentBlock.Text text
                && text.text() != null
                && !text.text().isBlank()) {
                texts.add(text.text());
            }
        }

        if (toolUse != null) {
            return parseToolUse(toolUse);
        }

        if (!texts.isEmpty()) {
            return new LlmResponse.Text(String.join("\n", texts));
        }

        throw new LlmResponseParseException("LLM text response is blank");
    }

    private LlmResponse parseToolUse(AnthropicMessageResponse.ContentBlock.ToolUse toolUse) {
        if (toolUse.name() == null || toolUse.name().isBlank()) {
            throw new LlmResponseParseException("LLM tool_use name is blank");
        }
        return new LlmResponse.ToolUse(toolUse.name(), toolUse.input());
    }
}
