package com.commerce.cs.infra.llm.client;

import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.llm.LlmRequest;
import com.commerce.cs.application.llm.LlmResponse;
import com.commerce.cs.infra.llm.fallback.FallbackTemplate;
import com.commerce.cs.infra.llm.parser.ResponseParser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicGateway anthropicGateway;
    private final ResponseParser responseParser;
    private final FallbackTemplate fallbackTemplate;

    @Override
    @Retry(name = "llm")
    @CircuitBreaker(name = "llm", fallbackMethod = "fallback")
    public LlmResponse call(LlmRequest request) {
        AnthropicMessageResponse response = anthropicGateway.createMessage(request);
        return responseParser.parse(response);
    }

    LlmResponse fallback(LlmRequest request, Throwable cause) {
        return fallbackTemplate.temporaryFailure(cause);
    }
}
