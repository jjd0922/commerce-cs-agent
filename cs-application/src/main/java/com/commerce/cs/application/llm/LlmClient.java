package com.commerce.cs.application.llm;

public interface LlmClient {

    LlmResponse call(LlmRequest request);
}
