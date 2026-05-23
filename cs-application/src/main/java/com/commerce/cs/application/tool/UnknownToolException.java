package com.commerce.cs.application.tool;

public final class UnknownToolException extends ToolExecutionException {

    public UnknownToolException(String toolName) {
        super("Unknown tool: " + toolName);
    }
}
