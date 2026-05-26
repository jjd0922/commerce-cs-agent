package com.commerce.cs.infra.llm.fallback;

import com.commerce.cs.application.llm.LlmResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackTemplate 임시 장애 응답")
class FallbackTemplateTest {

    @Test
    @DisplayName("LLM 장애 시 상담원 연결 안내를 포함한 텍스트 응답을 생성한다")
    void builds_temporary_failure_text_response() {
        LlmResponse response = new FallbackTemplate().temporaryFailure(new RuntimeException("timeout"));

        assertThat(response).isInstanceOf(LlmResponse.Text.class);
        assertThat(((LlmResponse.Text) response).text()).contains("human agent");
    }
}
