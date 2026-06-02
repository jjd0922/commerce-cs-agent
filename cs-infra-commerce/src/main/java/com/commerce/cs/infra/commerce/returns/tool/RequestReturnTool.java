package com.commerce.cs.infra.commerce.returns.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.returns.ReturnCommand;
import com.commerce.cs.application.returns.ReturnResult;
import com.commerce.cs.application.returns.ReturnUseCase;
import com.commerce.cs.application.tool.ToolHandler;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.returns.ReturnReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RequestReturnTool implements ToolHandler {

    private final ReturnUseCase returnUseCase;

    @Override
    public String name() {
        return "request_return";
    }

    @Override
    public String description() {
        return "Request a return for an authenticated customer's order.";
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public boolean mutation() {
        return true;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ChatContext context) {
        ReturnResult result = returnUseCase.requestReturn(new ReturnCommand(
            requiredText(args, "orderId"),
            authenticatedUserId(context),
            reason(args),
            optionalText(args, "detail"),
            idempotencyKey(context)
        ));
        return ToolResult.success(toResponse(result));
    }

    @Override
    public String buildConfirmationMessage(Map<String, Object> args) {
        return "Please confirm return request for order "
            + requiredText(args, "orderId")
            + " with reason "
            + reason(args)
            + ".";
    }

    @Override
    public String authenticationMessage() {
        return "Authentication is required to request a return.";
    }

    private Map<String, Object> toResponse(ReturnResult result) {
        return Map.of(
            "returnId", result.returnId(),
            "status", result.status().name(),
            "refundAmount", money(result.refundAmount()),
            "estimatedRefundAt", result.estimatedRefundAt().toString()
        );
    }

    private Map<String, Object> money(Money money) {
        return Map.of(
            "amount", money.amount(),
            "currency", money.currency()
        );
    }

    private String requiredText(Map<String, Object> args, String name) {
        Object value = args == null ? null : args.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.toString();
    }

    private String optionalText(Map<String, Object> args, String name) {
        Object value = args == null ? null : args.get(name);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    private ReturnReason reason(Map<String, Object> args) {
        String value = requiredText(args, "reason");
        try {
            return ReturnReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("reason must be one of DEFECT, WRONG_ITEM, CHANGED_MIND, OTHER", e);
        }
    }

    private String authenticatedUserId(ChatContext context) {
        if (context == null || context.userId() == null || context.userId().isBlank()) {
            throw new IllegalArgumentException("authenticated userId must not be blank");
        }
        return context.userId();
    }

    private String idempotencyKey(ChatContext context) {
        if (context == null || context.currentIdempotencyKey() == null || context.currentIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("current idempotency key must not be blank");
        }
        return context.currentIdempotencyKey();
    }
}
