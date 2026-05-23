package com.commerce.cs.application.chat;

import com.commerce.cs.application.tool.ToolResult;

public sealed interface ChatResult permits ChatResult.Text, ChatResult.RequiresAuthentication, ChatResult.RequiresConfirmation, ChatResult.ToolExecuted, ChatResult.Cancelled, ChatResult.Escalated {

    record Text(String message) implements ChatResult {
    }

    record RequiresAuthentication(String message) implements ChatResult {
    }

    record RequiresConfirmation(String message) implements ChatResult {
    }

    record ToolExecuted(ToolResult toolResult) implements ChatResult {
    }

    record Cancelled(String message) implements ChatResult {
    }

    record Escalated(String reason) implements ChatResult {
    }
}
