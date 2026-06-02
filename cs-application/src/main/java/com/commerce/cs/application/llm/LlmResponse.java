package com.commerce.cs.application.llm;

import java.util.Map;

public sealed interface LlmResponse permits LlmResponse.Text, LlmResponse.ToolUse {

    record Text(String text) implements LlmResponse {
    }

    record ToolUse(String toolName, Map<String, Object> args) implements LlmResponse {
        public ToolUse {
            args = args == null ? Map.of() : Map.copyOf(args);
        }
    }
}
