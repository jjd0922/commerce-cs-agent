package com.commerce.cs.application.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.idempotency.IdempotencyKeyBuilder;
import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ToolExecutionService implements ToolExecutor {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(5);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(3);

    private final Collection<ToolHandler> tools;
    private final IdempotencyStore idempotencyStore;
    private final DistributedLock distributedLock;

    @Override
    public ValidationResult validate(String toolName, Map<String, Object> args, ChatContext context) {
        ToolHandler tool = findTool(toolName);

        if (tool.requiresAuthentication() && !context.authenticated()) {
            return new ValidationResult.RequiresAuthentication(tool.authenticationMessage());
        }

        if (tool.mutation()) {
            return new ValidationResult.RequiresConfirmation(tool.buildConfirmationMessage(normalizeArgs(args)));
        }

        return new ValidationResult.Ok();
    }

    @Override
    public ToolResult execute(String toolName, Map<String, Object> args, ChatContext context) {
        ToolHandler tool = findTool(toolName);
        Map<String, Object> normalizedArgs = normalizeArgs(args);
        String idempotencyKey = IdempotencyKeyBuilder.build(context.sessionId(), toolName, normalizedArgs);

        return idempotencyStore.get(idempotencyKey)
            .orElseGet(() -> executeAndSave(tool, normalizedArgs, context.withCurrentIdempotencyKey(idempotencyKey), idempotencyKey));
    }

    private ToolResult executeAndSave(
        ToolHandler tool,
        Map<String, Object> args,
        ChatContext context,
        String idempotencyKey
    ) {
        Supplier<ToolResult> execution = () -> {
            ToolResult result = tool.execute(args, context);
            idempotencyStore.save(idempotencyKey, result, IDEMPOTENCY_TTL);
            return result;
        };

        if (!tool.mutation()) {
            return execution.get();
        }

        return distributedLock.withLock(lockKey(tool.name(), args), LOCK_TIMEOUT, execution);
    }

    private ToolHandler findTool(String toolName) {
        return tools.stream()
            .filter(tool -> tool.name().equals(toolName))
            .findFirst()
            .orElseThrow(() -> new UnknownToolException(toolName));
    }

    private Map<String, Object> normalizeArgs(Map<String, Object> args) {
        return args == null ? Map.of() : Map.copyOf(args);
    }

    private String lockKey(String toolName, Map<String, Object> args) {
        Object orderId = args.get("orderId");
        if (orderId == null) {
            return "tool:" + toolName;
        }
        return "tool:" + toolName + ":" + orderId;
    }
}
