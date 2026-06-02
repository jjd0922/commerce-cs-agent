package com.commerce.cs.infra.llm.client;

public final class LlmResponseParseException extends LlmClientException {

    public LlmResponseParseException(String message) {
        super(message);
    }
}
