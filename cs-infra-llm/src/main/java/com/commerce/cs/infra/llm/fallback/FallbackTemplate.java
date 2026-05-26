package com.commerce.cs.infra.llm.fallback;

import com.commerce.cs.application.llm.LlmResponse;
import org.springframework.stereotype.Component;

@Component
public class FallbackTemplate {

    public LlmResponse temporaryFailure(Throwable cause) {
        return new LlmResponse.Text("The automated response is temporarily unavailable. Please try again later or request a human agent.");
    }
}
