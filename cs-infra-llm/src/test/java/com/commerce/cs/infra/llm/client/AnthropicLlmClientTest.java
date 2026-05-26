package com.commerce.cs.infra.llm.client;

import com.commerce.cs.application.chat.ChatContext;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.infra.llm.fallback.FallbackTemplate;
import com.commerce.cs.infra.llm.parser.ResponseParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AnthropicLlmClient 응답 호출과 fallback 처리")
@ExtendWith(MockitoExtension.class)
class AnthropicLlmClientTest {

    @Mock
    private AnthropicGateway gateway;

    @Test
    @DisplayName("Gateway 응답을 호출하고 파싱된 ToolUse 응답을 반환한다")
    void calls_gateway_and_parses_response() {
        LlmRequest request = new LlmRequest(new ChatContext("session-1", null, false), "return policy");
        when(gateway.createMessage(same(request))).thenReturn(new AnthropicMessageResponse(
            List.of(new AnthropicMessageResponse.ContentBlock.ToolUse("search_faq", Map.of("query", request.userMessage())))
        ));
        AnthropicLlmClient client = new AnthropicLlmClient(gateway, new ResponseParser(), new FallbackTemplate());

        LlmResponse response = client.call(request);

        assertThat(response).isEqualTo(new LlmResponse.ToolUse("search_faq", Map.of("query", "return policy")));
        verify(gateway).createMessage(same(request));
    }

    @Test
    @DisplayName("fallback은 임시 장애 안내 텍스트 응답을 반환한다")
    void fallback_returns_template_response() {
        AnthropicLlmClient client = new AnthropicLlmClient(gateway, new ResponseParser(), new FallbackTemplate());

        LlmResponse response = client.fallback(
            new LlmRequest(new ChatContext("session-1", null, false), "hello"),
            new LlmClientException("timeout")
        );

        assertThat(response).isInstanceOf(LlmResponse.Text.class);
        assertThat(((LlmResponse.Text) response).text()).contains("human agent");
    }
}
