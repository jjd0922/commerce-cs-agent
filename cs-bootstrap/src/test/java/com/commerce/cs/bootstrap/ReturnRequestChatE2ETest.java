package com.commerce.cs.bootstrap;

import com.commerce.cs.api.chat.ChatResponse;
import com.commerce.cs.api.error.ApiErrorResponse;
import com.commerce.cs.api.session.VerificationResponse;
import com.commerce.cs.application.chat.SessionManager;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.bootstrap.demo.InMemoryOutboxPort;
import com.commerce.cs.bootstrap.demo.InMemoryReturnRepository;
import com.commerce.cs.infra.llm.client.AnthropicGateway;
import com.commerce.cs.infra.llm.client.AnthropicMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("반품 신청 Chat E2E")
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ReturnRequestChatE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InMemoryReturnRepository returnRepository;

    @Autowired
    private InMemoryOutboxPort outboxPort;

    @Autowired
    private SessionManager sessionManager;

    @MockBean
    private AnthropicGateway anthropicGateway;

    @BeforeEach
    void setUp() {
        when(anthropicGateway.createMessage(any(LlmRequest.class)))
            .thenAnswer(invocation -> requestReturn(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("미인증 반품 요청은 인증 요구 응답을 반환한다")
    void unauthenticated_return_request_requires_authentication() {
        ResponseEntity<ChatResponse> response = chat("session-unauth", "return order-1 because defect");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().type()).isEqualTo("REQUIRES_AUTHENTICATION");
        assertThat(response.getBody().message()).contains("Authentication");
        assertThat(sessionManager.loadContext("session-unauth").hasPendingAction()).isFalse();
        assertThat(returnRepository.count()).isZero();
        assertThat(outboxPort.events()).isEmpty();
    }

    @Test
    @DisplayName("인증 후 반품 요청은 confirmation을 요구하고 pending action을 저장한다")
    void authenticated_return_request_requires_confirmation() {
        verifySession("session-confirm");

        ResponseEntity<ChatResponse> response = chat("session-confirm", "return order-1 because defect");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().type()).isEqualTo("REQUIRES_CONFIRMATION");
        assertThat(response.getBody().message()).contains("order-1", "DEFECT");
        assertThat(sessionManager.loadContext("session-confirm").hasPendingAction()).isTrue();
        assertThat(returnRepository.count()).isZero();
        assertThat(outboxPort.events()).isEmpty();
    }

    @Test
    @DisplayName("confirmation 승인 후 반품을 생성하고 outbox event를 저장한 뒤 pending action을 제거한다")
    void confirmation_approval_creates_return_and_outbox_event() {
        verifySession("session-success");
        chat("session-success", "return order-1 because defect");

        ResponseEntity<ChatResponse> response = chat("session-success", "yes");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().type()).isEqualTo("TOOL_EXECUTED");
        assertThat(response.getBody().data())
            .containsKeys("returnId", "status", "refundAmount", "estimatedRefundAt")
            .containsEntry("status", "REQUESTED");
        assertThat(returnRepository.count()).isEqualTo(1);
        assertThat(outboxPort.events()).hasSize(1);
        assertThat(sessionManager.loadContext("session-success").hasPendingAction()).isFalse();
    }

    @Test
    @DisplayName("동일 반품 요청 재시도는 idempotency 결과를 반환하고 중복 생성하지 않는다")
    void repeated_return_request_does_not_create_duplicate_return_or_outbox_event() {
        verifySession("session-idem");
        chat("session-idem", "return order-1 because defect");
        ResponseEntity<ChatResponse> first = chat("session-idem", "yes");

        chat("session-idem", "return order-1 because defect");
        ResponseEntity<ChatResponse> second = chat("session-idem", "yes");

        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody().type()).isEqualTo("TOOL_EXECUTED");
        assertThat(second.getBody().data().get("returnId")).isEqualTo(first.getBody().data().get("returnId"));
        assertThat(returnRepository.count()).isEqualTo(1);
        assertThat(outboxPort.events()).hasSize(1);
        assertThat(sessionManager.loadContext("session-idem").hasPendingAction()).isFalse();
    }

    @Test
    @DisplayName("반품 불가 주문은 정책 오류 응답을 반환하고 side effect를 만들지 않는다")
    void non_returnable_order_returns_policy_error_without_side_effects() {
        verifySession("session-policy");
        chat("session-policy", "return order-2 because defect");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
            "/api/chat",
            Map.of("sessionId", "session-policy", "message", "yes"),
            ApiErrorResponse.class
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).contains("not returnable");
        assertThat(returnRepository.count()).isZero();
        assertThat(outboxPort.events()).isEmpty();
    }

    private void verifySession(String sessionId) {
        ResponseEntity<VerificationResponse> response = restTemplate.postForEntity(
            "/api/sessions/verify",
            Map.of(
                "sessionId", sessionId,
                "email", "customer@example.com",
                "phoneLast4", "5678"
            ),
            VerificationResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().userId()).isEqualTo("user-1");
    }

    private ResponseEntity<ChatResponse> chat(String sessionId, String message) {
        return restTemplate.postForEntity(
            "/api/chat",
            Map.of("sessionId", sessionId, "message", message),
            ChatResponse.class
        );
    }

    private AnthropicMessageResponse requestReturn(LlmRequest request) {
        String message = request.userMessage() == null ? "" : request.userMessage().toLowerCase();
        String orderId = message.contains("order-2") ? "order-2" : "order-1";
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.ToolUse(
                "request_return",
                Map.of(
                    "orderId", orderId,
                    "reason", "DEFECT",
                    "detail", "broken item"
                )
            )
        ));
    }
}
