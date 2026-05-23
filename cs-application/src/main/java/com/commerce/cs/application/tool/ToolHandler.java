package com.commerce.cs.application.tool;

import com.commerce.cs.application.chat.ChatContext;

import java.util.Map;

public interface ToolHandler {

    String name();

    String description();

    boolean requiresAuthentication();

    boolean mutation();

    ToolResult execute(Map<String, Object> args, ChatContext context);

    default String buildConfirmationMessage(Map<String, Object> args) {
        return "";
    }

    default String authenticationMessage() {
        return "Authentication is required.";
    }
}
