package com.commerce.cs.bootstrap;

import com.commerce.cs.api.chat.ChatResponse;
import com.commerce.cs.api.error.ApiErrorResponse;
import com.commerce.cs.bootstrap.demo.InMemoryOutboxPort;
import com.commerce.cs.bootstrap.demo.InMemoryReturnRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live-llm")
@DisplayName("Live LLM evaluation runner")
@ActiveProfiles({"demo", "live-llm"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class LiveLlmEvaluationRunnerTest {

    private static final String CASE_FILE = "/eval/live-llm-eval-cases.yml";
    private static final Path REPORT_DIR = Path.of("build", "reports", "eval");
    private static final Path JSON_REPORT = REPORT_DIR.resolve("live-llm-eval-result.json");
    private static final Path MARKDOWN_REPORT = REPORT_DIR.resolve("live-llm-eval-summary.md");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InMemoryReturnRepository returnRepository;

    @Autowired
    private InMemoryOutboxPort outboxPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("실제 LLM tool selection 측정 리포트를 생성한다")
    void run_live_llm_evaluation() throws Exception {
        List<EvalCase> cases = loadCases();
        long returnsBefore = returnRepository.count();
        int eventsBefore = outboxPort.events().size();

        List<CaseResult> results = cases.stream()
            .map(this::runCase)
            .toList();

        int unsafeMutationCount = (int) ((returnRepository.count() - returnsBefore)
            + (outboxPort.events().size() - eventsBefore));
        Summary summary = summarize(results, unsafeMutationCount);
        writeReports(summary);

        assertThat(unsafeMutationCount).isZero();
    }

    private CaseResult runCase(EvalCase evalCase) {
        String sessionId = "live-" + evalCase.id();
        long startedAt = System.nanoTime();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chat",
                Map.of("sessionId", sessionId, "message", evalCase.message()),
                String.class
            );
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            ObservedResponse observed = observed(response);
            return new CaseResult(
                evalCase.id(),
                evalCase.message(),
                evalCase.expectedResponseType(),
                evalCase.expectedTool(),
                observed.responseType(),
                observed.tool(),
                response.getStatusCode().value(),
                latencyMs,
                evalCase.expectedResponseType().equals(observed.responseType()),
                equalsNullable(evalCase.expectedTool(), observed.tool()),
                null
            );
        } catch (Exception e) {
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            return new CaseResult(
                evalCase.id(),
                evalCase.message(),
                evalCase.expectedResponseType(),
                evalCase.expectedTool(),
                "ERROR",
                null,
                0,
                latencyMs,
                false,
                false,
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }

    private ObservedResponse observed(ResponseEntity<String> response) throws Exception {
        if (response.getStatusCode().is2xxSuccessful()) {
            ChatResponse body = objectMapper.readValue(response.getBody(), ChatResponse.class);
            return new ObservedResponse(body.type(), inferTool(body));
        }
        ApiErrorResponse error = objectMapper.readValue(response.getBody(), ApiErrorResponse.class);
        return new ObservedResponse(error.code(), null);
    }

    private String inferTool(ChatResponse response) {
        if ("REQUIRES_AUTHENTICATION".equals(response.type()) || "REQUIRES_CONFIRMATION".equals(response.type())) {
            return "request_return";
        }
        if (!"TOOL_EXECUTED".equals(response.type()) || response.data() == null) {
            return null;
        }
        if (response.data().containsKey("answers")) {
            return "search_faq";
        }
        if (response.data().containsKey("policies")) {
            return "get_policy";
        }
        if (response.data().containsKey("products")) {
            return "search_product";
        }
        if (response.data().containsKey("returnId")) {
            return "request_return";
        }
        return null;
    }

    private Summary summarize(List<CaseResult> results, int unsafeMutationCount) {
        int total = results.size();
        long toolSelectionPassed = results.stream().filter(CaseResult::toolSelectionPassed).count();
        long responseTypePassed = results.stream().filter(CaseResult::responseTypePassed).count();
        long errorCount = results.stream().filter(result -> result.error() != null).count();
        double averageLatencyMs = results.stream()
            .mapToLong(CaseResult::latencyMs)
            .average()
            .orElse(0);
        long maxLatencyMs = results.stream()
            .mapToLong(CaseResult::latencyMs)
            .max()
            .orElse(0);

        return new Summary(
            Instant.now().toString(),
            System.getenv().getOrDefault("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001"),
            total,
            toolSelectionPassed,
            total == 0 ? 0 : (double) toolSelectionPassed / total,
            responseTypePassed,
            total == 0 ? 0 : (double) responseTypePassed / total,
            errorCount,
            unsafeMutationCount,
            averageLatencyMs,
            maxLatencyMs,
            results
        );
    }

    private void writeReports(Summary summary) throws Exception {
        Files.createDirectories(REPORT_DIR);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(JSON_REPORT.toFile(), summary);
        Files.writeString(MARKDOWN_REPORT, markdown(summary), StandardCharsets.UTF_8);
    }

    private String markdown(Summary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Live LLM Evaluation Summary\n\n");
        builder.append("| Metric | Value |\n");
        builder.append("|---|---:|\n");
        builder.append("| Model | `").append(summary.model()).append("` |\n");
        builder.append("| Total cases | ").append(summary.total()).append(" |\n");
        builder.append("| Tool selection accuracy | ").append(percent(summary.toolSelectionAccuracy())).append(" |\n");
        builder.append("| Response type accuracy | ").append(percent(summary.responseTypeAccuracy())).append(" |\n");
        builder.append("| Error count | ").append(summary.errorCount()).append(" |\n");
        builder.append("| Unsafe mutation count | ").append(summary.unsafeMutationCount()).append(" |\n");
        builder.append("| Average latency | ").append(String.format(Locale.ROOT, "%.1fms", summary.averageLatencyMs())).append(" |\n");
        builder.append("| Max latency | ").append(summary.maxLatencyMs()).append("ms |\n\n");

        builder.append("| Case | Expected Tool | Observed Tool | Expected Response | Observed Response | Latency | Result |\n");
        builder.append("|---|---|---|---|---|---:|---|\n");
        for (CaseResult result : summary.results()) {
            builder.append("| `").append(result.id()).append("` | ")
                .append(value(result.expectedTool())).append(" | ")
                .append(value(result.observedTool())).append(" | `")
                .append(result.expectedResponseType()).append("` | `")
                .append(result.observedResponseType()).append("` | ")
                .append(result.latencyMs()).append("ms | ")
                .append(result.toolSelectionPassed() && result.responseTypePassed() ? "PASS" : "CHECK")
                .append(" |\n");
        }
        return builder.toString();
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "none" : "`" + value + "`";
    }

    private boolean equalsNullable(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return actual == null || actual.isBlank();
        }
        return expected.equals(actual);
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
            throw new IllegalStateException("Failed to load live LLM evaluation cases", e);
        }
    }

    private EvalCase toCase(Map<String, Object> value) {
        return new EvalCase(
            text(value.get("id")),
            text(value.get("message")),
            text(value.get("expectedResponseType")),
            nullableText(value.get("expectedTool"))
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
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

    private record EvalCase(
        String id,
        String message,
        String expectedResponseType,
        String expectedTool
    ) {
    }

    private record ObservedResponse(String responseType, String tool) {
    }

    private record CaseResult(
        String id,
        String message,
        String expectedResponseType,
        String expectedTool,
        String observedResponseType,
        String observedTool,
        int httpStatus,
        long latencyMs,
        boolean responseTypePassed,
        boolean toolSelectionPassed,
        String error
    ) {
    }

    private record Summary(
        String executedAt,
        String model,
        int total,
        long toolSelectionPassed,
        double toolSelectionAccuracy,
        long responseTypePassed,
        double responseTypeAccuracy,
        long errorCount,
        int unsafeMutationCount,
        double averageLatencyMs,
        long maxLatencyMs,
        List<CaseResult> results
    ) {
    }
}
