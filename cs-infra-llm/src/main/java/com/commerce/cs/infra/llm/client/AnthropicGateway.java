package com.commerce.cs.infra.llm.client;

import com.commerce.cs.application.llm.LlmRequest;

public interface AnthropicGateway {

    AnthropicMessageResponse createMessage(LlmRequest request);
}
