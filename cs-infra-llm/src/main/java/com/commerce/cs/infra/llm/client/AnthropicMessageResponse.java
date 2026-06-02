package com.commerce.cs.infra.llm.client;

import java.util.List;
import java.util.Map;

public record AnthropicMessageResponse(
    List<ContentBlock> content
) {

    public AnthropicMessageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public sealed interface ContentBlock permits ContentBlock.Text, ContentBlock.ToolUse {

        record Text(String text) implements ContentBlock {
        }

        record ToolUse(String name, Map<String, Object> input) implements ContentBlock {
            public ToolUse {
                input = input == null ? Map.of() : Map.copyOf(input);
            }
        }
    }
}
