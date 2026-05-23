package com.commerce.cs.application.tool;

import com.commerce.cs.application.chat.ChatContext;

import java.util.Map;

public interface ToolExecutor {

    ValidationResult validate(String toolName, Map<String, Object> args, ChatContext context);

    ToolResult execute(String toolName, Map<String, Object> args, ChatContext context);
}
