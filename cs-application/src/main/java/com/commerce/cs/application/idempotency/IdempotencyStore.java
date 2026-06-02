package com.commerce.cs.application.idempotency;

import com.commerce.cs.application.tool.ToolResult;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<ToolResult> get(String key);

    void save(String key, ToolResult result, Duration ttl);
}
