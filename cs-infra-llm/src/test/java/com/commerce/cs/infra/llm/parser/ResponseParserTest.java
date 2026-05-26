package com.commerce.cs.infra.llm.parser;

import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import com.commerce.cs.infra.llm.client.LlmResponseParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResponseParser Anthropic 응답 파싱")
class ResponseParserTest {

    private final ResponseParser parser = new ResponseParser();

    @Test
    @DisplayName("텍스트 content block을 Text 응답으로 파싱한다")
    void parses_text_response() {
        LlmResponse response = parser.parse(new AnthropicMessageResponse(
            List.of(new AnthropicMessageResponse.ContentBlock.Text("hello"))
        ));

        assertThat(response).isEqualTo(new LlmResponse.Text("hello"));
    }

    @Test
    @DisplayName("tool_use content block을 ToolUse 응답으로 파싱한다")
    void parses_tool_use_response() {
        LlmResponse response = parser.parse(new AnthropicMessageResponse(
            List.of(new AnthropicMessageResponse.ContentBlock.ToolUse("get_order", Map.of("orderId", "order-1")))
        ));

        assertThat(response).isEqualTo(new LlmResponse.ToolUse("get_order", Map.of("orderId", "order-1")));
    }

    @Test
    @DisplayName("content가 비어 있으면 파싱 예외를 던진다")
    void fails_when_content_is_empty() {
        assertThatThrownBy(() -> parser.parse(new AnthropicMessageResponse(List.of())))
            .isInstanceOf(LlmResponseParseException.class)
            .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Tool 이름이 비어 있으면 파싱 예외를 던진다")
    void fails_when_tool_name_is_blank() {
        assertThatThrownBy(() -> parser.parse(new AnthropicMessageResponse(
            List.of(new AnthropicMessageResponse.ContentBlock.ToolUse("", Map.of()))
        )))
            .isInstanceOf(LlmResponseParseException.class)
            .hasMessageContaining("name");
    }
}
