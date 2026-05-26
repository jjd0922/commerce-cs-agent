package com.commerce.cs.application.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.idempotency.IdempotencyKeyBuilder;
import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.lock.DistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ToolExecutionService 실행 및 검증 흐름")
@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private DistributedLock distributedLock;

    @Test
    @DisplayName("인증이 필요한 Tool은 미인증 컨텍스트에서 본인확인을 요구한다")
    void authenticated_tool_requires_authentication_when_context_is_not_authenticated() {
        ToolHandler tool = tool("get_order", true, false);
        when(tool.authenticationMessage()).thenReturn("Authentication is required.");
        ToolExecutor executor = executorWith(tool);

        ValidationResult result = executor.validate(
            "get_order",
            Map.of("orderId", "order-1"),
            new ChatContext("session-1", null, false)
        );

        assertThat(result).isInstanceOf(ValidationResult.RequiresAuthentication.class);
        verify(tool).authenticationMessage();
    }

    @Test
    @DisplayName("변경성 Tool은 실행 전에 confirmation을 요구한다")
    void mutation_tool_requires_confirmation_before_execution() {
        ToolHandler tool = tool("request_return", true, true);
        when(tool.buildConfirmationMessage(Map.of("orderId", "order-1"))).thenReturn("confirm request_return");
        ToolExecutor executor = executorWith(tool);

        ValidationResult result = executor.validate(
            "request_return",
            Map.of("orderId", "order-1"),
            new ChatContext("session-1", "user-1", true)
        );

        assertThat(result).isInstanceOf(ValidationResult.RequiresConfirmation.class);
        assertThat(((ValidationResult.RequiresConfirmation) result).message()).contains("request_return");
        verify(tool).buildConfirmationMessage(Map.of("orderId", "order-1"));
    }

    @Test
    @DisplayName("검증을 통과한 조회 Tool은 바로 실행한다")
    void query_tool_can_be_executed_when_validation_passes() {
        ToolHandler tool = tool("search_faq", false, false);
        ToolResult expected = ToolResult.success(Map.of("tool", "search_faq"));
        when(idempotencyStore.get(anyString())).thenReturn(Optional.empty());
        when(tool.execute(eq(Map.of("query", "return policy")), any(ChatContext.class))).thenReturn(expected);
        ToolExecutor executor = executorWith(tool);

        ToolResult result = executor.execute(
            "search_faq",
            Map.of("query", "return policy"),
            new ChatContext("session-1", null, false)
        );

        assertThat(result.success()).isTrue();
        assertThat(result).isSameAs(expected);
        verify(tool).execute(
            eq(Map.of("query", "return policy")),
            argThat(context -> context.currentIdempotencyKey() != null)
        );
        verify(distributedLock, never()).withLock(anyString(), any(Duration.class), any());
    }

    @Test
    @DisplayName("동일한 인자와 세션의 반복 실행은 idempotency 캐시 결과를 반환한다")
    void repeated_execution_with_same_arguments_returns_cached_result() {
        ToolHandler tool = tool("request_return", true, true);
        ChatContext context = new ChatContext("session-1", "user-1", true);
        Map<String, Object> args = Map.of("orderId", "order-1", "reason", "DEFECT");
        ToolResult first = ToolResult.success(Map.of("returnId", "return-1"));
        String idempotencyKey = IdempotencyKeyBuilder.build(context.sessionId(), "request_return", args);

        when(idempotencyStore.get(idempotencyKey)).thenReturn(Optional.empty(), Optional.of(first));
        when(tool.execute(eq(args), any(ChatContext.class))).thenReturn(first);
        executeLockAction("tool:request_return:order-1");
        ToolExecutor executor = executorWith(tool);

        ToolResult created = executor.execute("request_return", args, context);
        ToolResult second = executor.execute("request_return", args, context);

        assertThat(created).isSameAs(first);
        assertThat(second).isSameAs(first);
        verify(tool, times(1)).execute(eq(args), any(ChatContext.class));
        verify(idempotencyStore).save(eq(idempotencyKey), same(first), any(Duration.class));
    }

    @Test
    @DisplayName("컨텍스트에 current idempotency key가 있으면 재계산하지 않고 해당 key로 실행한다")
    void execution_uses_current_idempotency_key_from_context() {
        ToolHandler tool = tool("request_return", true, true);
        ChatContext context = new ChatContext("session-1", "user-1", true)
            .withCurrentIdempotencyKey("pending-idem-key");
        Map<String, Object> args = Map.of("orderId", "order-1");
        ToolResult expected = ToolResult.success(Map.of("returnId", "return-1"));
        when(idempotencyStore.get("pending-idem-key")).thenReturn(Optional.empty());
        when(tool.execute(eq(args), any(ChatContext.class))).thenReturn(expected);
        executeLockAction("tool:request_return:order-1");
        ToolExecutor executor = executorWith(tool);

        ToolResult result = executor.execute("request_return", args, context);

        assertThat(result).isSameAs(expected);
        verify(idempotencyStore).get("pending-idem-key");
        verify(idempotencyStore).save(eq("pending-idem-key"), same(expected), any(Duration.class));
        verify(tool).execute(
            eq(args),
            argThat(chatContext -> "pending-idem-key".equals(chatContext.currentIdempotencyKey()))
        );
    }

    @Test
    @DisplayName("변경성 Tool은 주문 단위 분산락 안에서 실행한다")
    void mutation_tool_is_executed_with_distributed_lock() {
        ToolHandler tool = tool("request_return", true, true);
        ToolResult expected = ToolResult.success(Map.of("returnId", "return-1"));
        when(idempotencyStore.get(anyString())).thenReturn(Optional.empty());
        when(tool.execute(eq(Map.of("orderId", "order-1")), any(ChatContext.class))).thenReturn(expected);
        executeLockAction("tool:request_return:order-1");
        ToolExecutor executor = executorWith(tool);

        ToolResult result = executor.execute(
            "request_return",
            Map.of("orderId", "order-1"),
            new ChatContext("session-1", "user-1", true)
        );

        assertThat(result).isSameAs(expected);
        verify(distributedLock).withLock(
            eq("tool:request_return:order-1"),
            eq(Duration.ofSeconds(3)),
            any()
        );
    }

    @Test
    @DisplayName("등록되지 않은 Tool 이름은 UnknownToolException을 던진다")
    void unknown_tool_throws_exception() {
        ToolExecutor executor = executorWith();

        assertThatThrownBy(() -> executor.validate("missing", Map.of(), new ChatContext("session-1", null, false)))
            .isInstanceOf(UnknownToolException.class)
            .hasMessageContaining("missing");

        verifyNoInteractions(idempotencyStore, distributedLock);
    }

    private ToolExecutor executorWith(ToolHandler... tools) {
        return new ToolExecutionService(List.of(tools), idempotencyStore, distributedLock);
    }

    private ToolHandler tool(String name, boolean requiresAuthentication, boolean mutation) {
        ToolHandler tool = mock(ToolHandler.class);
        when(tool.name()).thenReturn(name);
        lenient().when(tool.requiresAuthentication()).thenReturn(requiresAuthentication);
        lenient().when(tool.mutation()).thenReturn(mutation);
        return tool;
    }

    private void executeLockAction(String lockKey) {
        when(distributedLock.withLock(eq(lockKey), any(Duration.class), any()))
            .thenAnswer(invocation -> {
                Supplier<ToolResult> action = invocation.getArgument(2);
                return action.get();
            });
    }
}
