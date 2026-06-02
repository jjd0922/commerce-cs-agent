package com.commerce.cs.application.chat;

import com.commerce.cs.application.idempotency.IdempotencyKeyBuilder;
import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.application.tool.ToolExecutor;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.application.tool.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final SessionManager sessionManager;
    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final Clock clock;

    @Override
    public ChatResult handle(ChatCommand command) {
        SessionContext sessionContext = sessionManager.loadContext(command.sessionId());
        sessionManager.appendUserMessage(command.sessionId(), command.message());
        if (sessionContext.hasPendingAction()) {
            return handlePendingAction(command, sessionContext);
        }

        LlmResponse response = llmClient.call(new LlmRequest(
            sessionContext.chatContext(),
            command.message(),
            sessionContext.history()
        ));
        if (response instanceof LlmResponse.Text text) {
            sessionManager.appendAssistantMessage(command.sessionId(), text.text());
            return new ChatResult.Text(text.text());
        }
        if (response instanceof LlmResponse.ToolUse toolUse) {
            return handleToolUse(command.sessionId(), sessionContext.chatContext(), toolUse);
        }

        return new ChatResult.Escalated("Unsupported LLM response");
    }

    private ChatResult handlePendingAction(ChatCommand command, SessionContext sessionContext) {
        ConfirmationDecision decision = ConfirmationDecider.decide(command.message());
        PendingAction pendingAction = sessionContext.pendingAction();

        if (decision == ConfirmationDecision.REJECT) {
            sessionManager.clearPendingAction(command.sessionId());
            sessionManager.appendAssistantMessage(command.sessionId(), "Pending action was cancelled.");
            return new ChatResult.Cancelled("Pending action was cancelled.");
        }

        if (decision == ConfirmationDecision.UNKNOWN) {
            sessionManager.appendAssistantMessage(command.sessionId(), "Please answer yes or no.");
            return new ChatResult.RequiresConfirmation("Please answer yes or no.");
        }

        ValidationResult validation = toolExecutor.validate(
            pendingAction.toolName(),
            pendingAction.args(),
            sessionContext.chatContext()
        );

        if (validation instanceof ValidationResult.RequiresAuthentication requiresAuthentication) {
            sessionManager.appendAssistantMessage(command.sessionId(), requiresAuthentication.message());
            return new ChatResult.RequiresAuthentication(requiresAuthentication.message());
        }

        ToolResult result = toolExecutor.execute(
            pendingAction.toolName(),
            pendingAction.args(),
            sessionContext.chatContext().withCurrentIdempotencyKey(pendingAction.idempotencyKey())
        );
        sessionManager.clearPendingAction(command.sessionId());
        return new ChatResult.ToolExecuted(result);
    }

    private ChatResult handleToolUse(String sessionId, ChatContext context, LlmResponse.ToolUse toolUse) {
        ValidationResult validation = toolExecutor.validate(toolUse.toolName(), toolUse.args(), context);

        if (validation instanceof ValidationResult.RequiresAuthentication requiresAuthentication) {
            sessionManager.appendAssistantMessage(sessionId, requiresAuthentication.message());
            return new ChatResult.RequiresAuthentication(requiresAuthentication.message());
        }

        if (validation instanceof ValidationResult.RequiresConfirmation requiresConfirmation) {
            PendingAction pendingAction = new PendingAction(
                toolUse.toolName(),
                toolUse.args(),
                IdempotencyKeyBuilder.build(context.sessionId(), toolUse.toolName(), toolUse.args()),
                clock.instant()
            );
            sessionManager.savePendingAction(sessionId, pendingAction);
            sessionManager.appendAssistantMessage(sessionId, requiresConfirmation.message());
            return new ChatResult.RequiresConfirmation(requiresConfirmation.message());
        }

        ToolResult result = toolExecutor.execute(toolUse.toolName(), toolUse.args(), context);
        return new ChatResult.ToolExecuted(result);
    }
}
