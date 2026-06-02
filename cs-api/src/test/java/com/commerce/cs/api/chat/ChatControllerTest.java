package com.commerce.cs.api.chat;

import com.commerce.cs.application.chat.ChatResult;
import com.commerce.cs.application.chat.ChatUseCase;
import com.commerce.cs.application.tool.ToolResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ChatController 응답 매핑")
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatUseCase chatUseCase;

    @Test
    @DisplayName("텍스트 채팅 결과를 TEXT 응답으로 변환한다")
    void chat_returns_text_response() {
        when(chatUseCase.handle(argThat(command ->
            command.sessionId().equals("session-1") && command.message().equals("hi")
        ))).thenReturn(new ChatResult.Text("hello"));
        ChatController controller = new ChatController(chatUseCase);

        ChatResponse response = controller.chat(new ChatRequest("session-1", "hi")).getBody();

        assertThat(response).isEqualTo(ChatResponse.text("hello"));
        verify(chatUseCase).handle(argThat(command ->
            command.sessionId().equals("session-1") && command.message().equals("hi")
        ));
    }

    @Test
    @DisplayName("confirmation 요청 결과를 REQUIRES_CONFIRMATION 응답으로 변환한다")
    void chat_maps_confirmation_response() {
        when(chatUseCase.handle(argThat(command -> command.message().equals("return"))))
            .thenReturn(new ChatResult.RequiresConfirmation("confirm request"));
        ChatController controller = new ChatController(chatUseCase);

        ChatResponse response = controller.chat(new ChatRequest("session-1", "return")).getBody();

        assertThat(response).isEqualTo(ChatResponse.requiresConfirmation("confirm request"));
    }

    @Test
    @DisplayName("Tool 실행 결과 data를 TOOL_EXECUTED 응답에 담아 반환한다")
    void chat_maps_tool_result_data() {
        when(chatUseCase.handle(argThat(command -> command.message().equals("confirm"))))
            .thenReturn(new ChatResult.ToolExecuted(ToolResult.success(Map.of("returnId", "return-1"))));
        ChatController controller = new ChatController(chatUseCase);

        ChatResponse response = controller.chat(new ChatRequest("session-1", "confirm")).getBody();

        assertThat(response.type()).isEqualTo("TOOL_EXECUTED");
        assertThat(response.data()).containsEntry("returnId", "return-1");
    }
}
