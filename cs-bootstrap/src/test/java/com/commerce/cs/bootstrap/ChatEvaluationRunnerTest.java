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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@DisplayName("Chat evaluation runner")
@ActiveProfiles("demo")
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ChatEvaluationRunnerTest {

    private static final String CASE_FILE = "/eval/chat-eval-cases.yml";

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

    @MockBean
    private Clock clock;

    private final Map<String, List<String>> toolsBySession = new ConcurrentHashMap<>();

    @TestFactory
    @DisplayName("chat evaluation cases")
    Stream<DynamicTest> chatEvaluationCases() {
        return loadCases().stream()
            .map(evalCase -> DynamicTest.dynamicTest(
                evalCase.id() + " - " + evalCase.description(),
                () -> runCase(evalCase)
            ));
    }

    @Test
    @DisplayName("evaluation catalog contains the minimum portfolio categories")
    void evaluation_catalog_contains_minimum_categories() {
        List<EvalCase> cases = loadCases();

        assertThat(cases)
            .extracting(EvalCase::id)
            .containsExactly(
                "faq-search",
                "policy-search",
                "product-search",
                "auth-required",
                "return-confirmation",
                "return-execution",
                "return-policy-violation",
                "idempotency",
                "llm-fallback"
            );
        System.out.println("chat-eval-cases total=" + cases.size()
            + " ids=" + cases.stream().map(EvalCase::id).toList());
    }

    private void runCase(EvalCase evalCase) {
        reset(anthropicGateway);
        reset(clock);
        toolsBySession.clear();
        when(clock.instant()).thenReturn(Instant.parse("2026-05-22T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(anthropicGateway.createMessage(any(LlmRequest.class)))
            .thenAnswer(invocation -> fakeLlm(invocation.getArgument(0)));

        String sessionId = evalCase.givenSession().sessionId();
        if (evalCase.givenSession().authenticated()) {
            verifySession(sessionId);
        }

        long returnsBefore = returnRepository.count();
        int eventsBefore = outboxPort.events().size();
        ResponseEntity<?> lastResponse = null;

        for (int i = 0; i < evalCase.messages().size(); i++) {
            boolean lastMessage = i == evalCase.messages().size() - 1;
            lastResponse = postChat(
                sessionId,
                evalCase.messages().get(i),
                lastMessage ? evalCase.expectedResponseType() : null
            );
        }

        assertThat(lastResponse).isNotNull();
        assertResponse(evalCase, lastResponse);
        assertExpectedTool(evalCase, sessionId);
        assertSideEffects(evalCase, sessionId, returnsBefore, eventsBefore);
    }

    private ResponseEntity<?> postChat(String sessionId, String message, String expectedResponseType) {
        if ("BAD_REQUEST".equals(expectedResponseType)) {
            return restTemplate.postForEntity(
                "/api/chat",
                Map.of("sessionId", sessionId, "message", message),
                ApiErrorResponse.class
            );
        }
        return restTemplate.postForEntity(
            "/api/chat",
            Map.of("sessionId", sessionId, "message", message),
            ChatResponse.class
        );
    }

    private void assertResponse(EvalCase evalCase, ResponseEntity<?> response) {
        if ("BAD_REQUEST".equals(evalCase.expectedResponseType())) {
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
            ApiErrorResponse body = (ApiErrorResponse) response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.code()).isEqualTo("BAD_REQUEST");
            return;
        }

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ChatResponse body = (ChatResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.type()).isEqualTo(evalCase.expectedResponseType());
    }

    private void assertExpectedTool(EvalCase evalCase, String sessionId) {
        if (evalCase.expectedTool() == null || evalCase.expectedTool().isBlank()) {
            assertThat(toolsBySession.getOrDefault(sessionId, List.of())).isEmpty();
            return;
        }
        assertThat(toolsBySession.getOrDefault(sessionId, List.of()))
            .contains(evalCase.expectedTool());
    }

    private void assertSideEffects(EvalCase evalCase, String sessionId, long returnsBefore, int eventsBefore) {
        ExpectedSideEffects expected = evalCase.expectedSideEffects();
        assertThat(returnRepository.count() - returnsBefore).isEqualTo(expected.returnCountDelta());
        assertThat(outboxPort.events().size() - eventsBefore).isEqualTo(expected.outboxEventDelta());
        assertThat(sessionManager.loadContext(sessionId).hasPendingAction()).isEqualTo(expected.pendingAction());
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
    }

    private AnthropicMessageResponse fakeLlm(LlmRequest request) {
        String sessionId = request.context().sessionId();
        String message = request.userMessage() == null ? "" : request.userMessage().toLowerCase(Locale.ROOT);

        if (message.contains("fallback")) {
            return text("The automated response is temporarily unavailable. Please try again later or request a human agent.");
        }
        if (message.contains("faq")) {
            return tool(sessionId, "search_faq", Map.of("query", request.userMessage()));
        }
        if (message.contains("policy") && !message.contains("order-2")) {
            return tool(sessionId, "get_policy", Map.of("query", request.userMessage()));
        }
        if (message.contains("product")) {
            return tool(sessionId, "search_product", Map.of("query", request.userMessage()));
        }
        if (message.contains("return")) {
            return tool(sessionId, "request_return", Map.of(
                "orderId", message.contains("order-2") ? "order-2" : "order-1",
                "reason", "DEFECT",
                "detail", "broken item"
            ));
        }
        return text("Demo assistant response: " + request.userMessage());
    }

    private AnthropicMessageResponse tool(String sessionId, String toolName, Map<String, Object> args) {
        toolsBySession.computeIfAbsent(sessionId, ignored -> new ArrayList<>()).add(toolName);
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.ToolUse(toolName, args)
        ));
    }

    private AnthropicMessageResponse text(String text) {
        return new AnthropicMessageResponse(List.of(
            new AnthropicMessageResponse.ContentBlock.Text(text)
        ));
    }

    private List<EvalCase> loadCases() {
        try (InputStream inputStream = getClass().getResourceAsStream(CASE_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Evaluation case file not found: " + CASE_FILE);
            }
            Map<String, Object> root = new Yaml().load(inputStream);
            return list(root.get("cases")).stream()
                .map(this::toCase)
                .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load evaluation cases", e);
        }
    }

    private EvalCase toCase(Map<String, Object> value) {
        Map<String, Object> givenSession = map(value.get("givenSession"));
        Map<String, Object> sideEffects = map(value.get("expectedSideEffects"));
        return new EvalCase(
            text(value.get("id")),
            text(value.get("description")),
            text(value.get("profile")),
            new GivenSession(
                text(givenSession.get("sessionId")),
                bool(givenSession.get("authenticated"))
            ),
            strings(value.get("messages")),
            text(value.get("expectedResponseType")),
            nullableText(value.get("expectedTool")),
            new ExpectedSideEffects(
                integer(sideEffects.get("returnCountDelta")),
                integer(sideEffects.get("outboxEventDelta")),
                bool(sideEffects.get("pendingAction"))
            )
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object value) {
        return ((List<Object>) value).stream()
            .map(this::text)
            .toList();
    }

    private String text(Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return value.toString();
    }

    private String nullableText(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(text(value));
    }

    private int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(text(value));
    }

    private record EvalCase(
        String id,
        String description,
        String profile,
        GivenSession givenSession,
        List<String> messages,
        String expectedResponseType,
        String expectedTool,
        ExpectedSideEffects expectedSideEffects
    ) {
    }

    private record GivenSession(String sessionId, boolean authenticated) {
    }

    private record ExpectedSideEffects(int returnCountDelta, int outboxEventDelta, boolean pendingAction) {
    }
}
