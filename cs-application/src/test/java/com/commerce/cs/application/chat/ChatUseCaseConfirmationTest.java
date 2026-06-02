package com.commerce.cs.application.chat;

import com.commerce.cs.application.idempotency.IdempotencyKeyBuilder;
import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.application.lock.DistributedLock;
import com.commerce.cs.application.tool.ToolExecutor;
import com.commerce.cs.application.tool.ToolHandler;
import com.commerce.cs.application.tool.ToolResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    private IdempotencyStore idempotencyStore;

    @Mock
    private DistributedLock distributedLock;

    @Test
    @DisplayName("변경성 Tool 호출은 PendingAction으로 저장하고 confirmation 응답을 반환한다")
    void mutation_tool_use_is_saved_as_pending_action_and_returns_confirmation() {
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1");
        ToolHandler tool = tool("request_return", true, true);
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, null));
        when(llmClient.call(any(LlmRequest.class))).thenReturn(new LlmResponse.ToolUse("request_return", args));
        when(tool.buildConfirmationMessage(args)).thenReturn("confirm request_return");
        ChatUseCase useCase = useCaseWith(tool);

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
            )
        );
        verify(tool, never()).execute(any(), any());
    }

    @Test
    @DisplayName("PendingAction 승인 응답은 Tool을 실행하고 PendingAction을 제거한다")
    void approving_pending_action_executes_tool_and_clears_pending_action() {
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1");
        PendingAction pendingAction = pendingAction(context, "request_return", args);
        ToolHandler tool = tool("request_return", true, true);
        ToolResult toolResult = ToolResult.success(Map.of("tool", "request_return"));
        when(sessionManager.loadContext("session-1"))
            .thenReturn(new SessionContext(context, null), new SessionContext(context, pendingAction));
        when(llmClient.call(any(LlmRequest.class))).thenReturn(new LlmResponse.ToolUse("request_return", args));
        when(tool.buildConfirmationMessage(args)).thenReturn("confirm request_return");
        when(idempotencyStore.get(pendingAction.idempotencyKey())).thenReturn(Optional.empty());
        when(tool.execute(eq(args), any(ChatContext.class))).thenReturn(toolResult);
        executeLockAction("tool:request_return:order-1");
        ChatUseCase useCase = useCaseWith(tool);

        useCase.handle(new ChatCommand("session-1", "return this order"));
        ChatResult approved = useCase.handle(new ChatCommand("session-1", "확인"));

        assertThat(approved).isInstanceOf(ChatResult.ToolExecuted.class);
        verify(tool).execute(
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
        ToolHandler tool = tool("request_return", true, true);
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, pendingAction));
        ChatUseCase useCase = useCaseWith(tool);

        ChatResult rejected = useCase.handle(new ChatCommand("session-1", "취소"));

        assertThat(rejected).isInstanceOf(ChatResult.Cancelled.class);
        verify(sessionManager).clearPendingAction("session-1");
        verify(tool, never()).execute(any(), any());
        verifyNoInteractions(llmClient, idempotencyStore, distributedLock);
    }

    @Test
    @DisplayName("LLM 텍스트 응답은 세션에 assistant 메시지로 저장하고 그대로 반환한다")
    void text_response_is_returned_and_appended_to_session() {
        ChatContext context = new ChatContext("session-1", null, false);
        ToolHandler tool = tool("search_faq", false, false);
        when(sessionManager.loadContext("session-1")).thenReturn(new SessionContext(context, null));
        when(llmClient.call(any(LlmRequest.class))).thenReturn(new LlmResponse.Text("Hello"));
        ChatUseCase useCase = useCaseWith(tool);

        ChatResult result = useCase.handle(new ChatCommand("session-1", "hello"));

        assertThat(result).isEqualTo(new ChatResult.Text("Hello"));
        verify(sessionManager).appendAssistantMessage("session-1", "Hello");
        verify(tool, never()).execute(any(), any());
    }

    private ChatUseCase useCaseWith(ToolHandler tool) {
        return new ChatUseCase(
            sessionManager,
            llmClient,
            new ToolExecutor(List.of(tool), idempotencyStore, distributedLock)
        );
    }

    private ToolHandler tool(String name, boolean requiresAuthentication, boolean mutation) {
        ToolHandler tool = mock(ToolHandler.class);
        when(tool.name()).thenReturn(name);
        lenient().when(tool.requiresAuthentication()).thenReturn(requiresAuthentication);
        lenient().when(tool.mutation()).thenReturn(mutation);
        return tool;
    }

    private PendingAction pendingAction(ChatContext context, String toolName, Map<String, Object> args) {
        return new PendingAction(
            toolName,
            args,
            IdempotencyKeyBuilder.build(context.sessionId(), toolName, args),
            Instant.now()
        );
    }

    private void executeLockAction(String lockKey) {
        when(distributedLock.withLock(eq(lockKey), any(Duration.class), any()))
            .thenAnswer(invocation -> {
                Supplier<ToolResult> action = invocation.getArgument(2);
                return action.get();
            });
    }
}
