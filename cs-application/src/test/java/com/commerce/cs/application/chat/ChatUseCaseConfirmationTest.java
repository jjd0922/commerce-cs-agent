package com.commerce.cs.application.chat;

import com.commerce.cs.application.idempotency.IdempotencyKeyBuilder;
import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.application.tool.ToolExecutor;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.application.tool.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ChatUseCase confirmation 처리 흐름")
@ExtendWith(MockitoExtension.class)
class ChatUseCaseConfirmationTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private LlmClient llmClient;

    @Mock
    private ToolExecutor toolExecutor;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("변경성 Tool 호출은 PendingAction으로 저장하고 confirmation 응답을 반환한다")
    void mutation_tool_use_is_saved_as_pending_action_and_returns_confirmation() {
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1");
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, null));
        when(llmClient.call(any(LlmRequest.class))).thenReturn(new LlmResponse.ToolUse("request_return", args));
        when(toolExecutor.validate("request_return", args, context))
            .thenReturn(new ValidationResult.RequiresConfirmation("confirm request_return"));
        ChatUseCase useCase = useCase();

        ChatResult result = useCase.handle(new ChatCommand("session-1", "return this order"));

        assertThat(result).isInstanceOf(ChatResult.RequiresConfirmation.class);
        verify(sessionManager).savePendingAction(
            eq("session-1"),
            argThat(pendingAction ->
                pendingAction.toolName().equals("request_return")
                    && pendingAction.args().equals(args)
                    && pendingAction.idempotencyKey().equals(
                    IdempotencyKeyBuilder.build(context.sessionId(), "request_return", args)
                )
                    && pendingAction.createdAt().equals(clock.instant())
            )
        );
        verify(toolExecutor, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("PendingAction 승인 응답은 Tool을 실행하고 PendingAction을 제거한다")
    void approving_pending_action_executes_tool_and_clears_pending_action() {
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1");
        PendingAction pendingAction = pendingAction(context, "request_return", args);
        ToolResult toolResult = ToolResult.success(Map.of("tool", "request_return"));
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, pendingAction));
        when(toolExecutor.validate("request_return", args, context)).thenReturn(new ValidationResult.Ok());
        when(toolExecutor.execute(eq("request_return"), eq(args), any(ChatContext.class))).thenReturn(toolResult);
        ChatUseCase useCase = useCase();

        ChatResult approved = useCase.handle(new ChatCommand("session-1", "확인"));

        assertThat(approved).isInstanceOf(ChatResult.ToolExecuted.class);
        verify(toolExecutor).execute(
            eq("request_return"),
            eq(args),
            argThat(chatContext -> pendingAction.idempotencyKey().equals(chatContext.currentIdempotencyKey()))
        );
        verify(sessionManager).clearPendingAction("session-1");
    }

    @Test
    @DisplayName("PendingAction 거절 응답은 Tool 실행 없이 PendingAction만 제거한다")
    void rejecting_pending_action_clears_pending_action_without_execution() {
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1");
        PendingAction pendingAction = pendingAction(context, "request_return", args);
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, pendingAction));
        ChatUseCase useCase = useCase();

        ChatResult rejected = useCase.handle(new ChatCommand("session-1", "취소"));

        assertThat(rejected).isInstanceOf(ChatResult.Cancelled.class);
        verify(sessionManager).clearPendingAction("session-1");
        verifyNoInteractions(llmClient, toolExecutor);
    }

    @Test
    @DisplayName("LLM 텍스트 응답은 세션에 assistant 메시지로 저장하고 그대로 반환한다")
    void text_response_is_returned_and_appended_to_session() {
        ChatContext context = new ChatContext("session-1", null, false);
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, null));
        when(llmClient.call(any(LlmRequest.class))).thenReturn(new LlmResponse.Text("Hello"));
        ChatUseCase useCase = useCase();

        ChatResult result = useCase.handle(new ChatCommand("session-1", "hello"));

        assertThat(result).isEqualTo(new ChatResult.Text("Hello"));
        verify(sessionManager).appendUserMessage("session-1", "hello");
        verify(sessionManager).appendAssistantMessage("session-1", "Hello");
        verifyNoInteractions(toolExecutor);
    }

    @Test
    @DisplayName("LLM 요청에는 현재 세션의 최근 대화 이력을 포함한다")
    void llm_request_contains_session_history() {
        ChatContext context = new ChatContext("session-1", null, false);
        List<ChatMessage> history = List.of(
            new ChatMessage(ChatMessage.Role.USER, "I want to return my order."),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Please share your order id.")
        );
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, null, history));
        when(llmClient.call(argThat(request ->
            request.userMessage().equals("order-1")
                && request.history().equals(history)
        ))).thenReturn(new LlmResponse.Text("I can help with that."));
        ChatUseCase useCase = useCase();

        ChatResult result = useCase.handle(new ChatCommand("session-1", "order-1"));

        assertThat(result).isEqualTo(new ChatResult.Text("I can help with that."));
        verify(sessionManager).appendUserMessage("session-1", "order-1");
    }

    private ChatUseCase useCase() {
        return new ChatService(sessionManager, llmClient, toolExecutor, clock);
    }

    private PendingAction pendingAction(ChatContext context, String toolName, Map<String, Object> args) {
        return new PendingAction(
            toolName,
            args,
            IdempotencyKeyBuilder.build(context.sessionId(), toolName, args),
            Instant.now()
        );
    }
}
