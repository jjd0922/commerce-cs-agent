package com.commerce.cs.infra.commerce.returns.tool;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.returns.ReturnCommand;
import com.commerce.cs.application.returns.ReturnResult;
import com.commerce.cs.application.returns.ReturnUseCase;
import com.commerce.cs.application.tool.ToolResult;
import com.commerce.cs.domain.common.Money;
import com.commerce.cs.domain.returns.ReturnReason;
import com.commerce.cs.domain.returns.ReturnStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RequestReturnTool")
@ExtendWith(MockitoExtension.class)
class RequestReturnToolTest {

    @Mock
    private ReturnUseCase returnUseCase;

    @Test
    @DisplayName("authenticated mutation tool metadata를 제공한다")
    void exposes_authenticated_mutation_tool_metadata() {
        RequestReturnTool tool = tool();

        assertThat(tool.name()).isEqualTo("request_return");
        assertThat(tool.requiresAuthentication()).isTrue();
        assertThat(tool.mutation()).isTrue();
        assertThat(tool.authenticationMessage()).contains("Authentication");
    }

    @Test
    @DisplayName("confirmation message에 orderId와 reason을 포함한다")
    void builds_confirmation_message() {
        RequestReturnTool tool = tool();

        String message = tool.buildConfirmationMessage(Map.of(
            "orderId", "order-1",
            "reason", "defect"
        ));

        assertThat(message).contains("order-1", "DEFECT");
    }

    @Test
    @DisplayName("정상 args로 ReturnUseCase를 호출하고 표준 응답 schema를 반환한다")
    void executes_return_use_case_with_valid_args() {
        RequestReturnTool tool = tool();
        when(returnUseCase.requestReturn(org.mockito.ArgumentMatchers.any(ReturnCommand.class)))
            .thenReturn(new ReturnResult(
                "return-1",
                ReturnStatus.REQUESTED,
                Money.won(100_000),
                Instant.parse("2026-05-29T00:00:00Z")
            ));

        ToolResult result = tool.execute(
            Map.of("orderId", "order-1", "reason", "wrong_item", "detail", "wrong size"),
            context()
        );

        ArgumentCaptor<ReturnCommand> commandCaptor = ArgumentCaptor.forClass(ReturnCommand.class);
        verify(returnUseCase).requestReturn(commandCaptor.capture());
        ReturnCommand command = commandCaptor.getValue();
        assertThat(command.orderId()).isEqualTo("order-1");
        assertThat(command.userId()).isEqualTo("user-1");
        assertThat(command.reason()).isEqualTo(ReturnReason.WRONG_ITEM);
        assertThat(command.detail()).isEqualTo("wrong size");
        assertThat(command.idempotencyKey()).isEqualTo("idem-1");

        assertThat(result.success()).isTrue();
        assertThat(result.data())
            .containsEntry("returnId", "return-1")
            .containsEntry("status", "REQUESTED")
            .containsEntry("estimatedRefundAt", "2026-05-29T00:00:00Z");
        assertThat(result.data().get("refundAmount"))
            .isEqualTo(Map.of("amount", Money.won(100_000).amount(), "currency", "KRW"));
    }

    @Test
    @DisplayName("orderId가 없으면 실패한다")
    void fails_when_order_id_is_missing() {
        RequestReturnTool tool = tool();

        assertThatThrownBy(() -> tool.execute(Map.of("reason", "DEFECT"), context()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("orderId");
        verifyNoInteractions(returnUseCase);
    }

    @Test
    @DisplayName("reason이 없거나 enum 값이 아니면 실패한다")
    void fails_when_reason_is_invalid() {
        RequestReturnTool tool = tool();

        assertThatThrownBy(() -> tool.execute(Map.of("orderId", "order-1"), context()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");
        assertThatThrownBy(() -> tool.execute(Map.of("orderId", "order-1", "reason", "lost"), context()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");
        verifyNoInteractions(returnUseCase);
    }

    private RequestReturnTool tool() {
        return new RequestReturnTool(returnUseCase);
    }

    private ChatContext context() {
        return new ChatContext("session-1", "user-1", true)
            .withCurrentIdempotencyKey("idem-1");
    }
}
