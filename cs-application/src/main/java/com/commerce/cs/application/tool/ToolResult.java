package com.commerce.cs.application.tool;

import java.util.Map;

public final class ToolResult {

    private final boolean success;
    private final Map<String, Object> data;
    private final String message;

    private ToolResult(boolean success, Map<String, Object> data, String message) {
        this.success = success;
        this.data = data == null ? Map.of() : Map.copyOf(data);
        this.message = message;
    }

    public static ToolResult success(Map<String, Object> data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult failure(String message) {
        return new ToolResult(false, Map.of(), message);
    }

    public boolean success() {
        return success;
    }

    public Map<String, Object> data() {
        return data;
    }

    public String message() {
        return message;
    }
}
