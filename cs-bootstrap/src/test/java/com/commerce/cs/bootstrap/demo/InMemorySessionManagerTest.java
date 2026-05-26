package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.chat.ChatMessage;
import com.commerce.cs.application.chat.SessionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemorySessionManager chat history 관리")
class InMemorySessionManagerTest {

    @Test
    @DisplayName("user와 assistant 메시지를 세션 이력에 순서대로 저장한다")
    void stores_user_and_assistant_messages_in_order() {
        InMemorySessionManager sessionManager = new InMemorySessionManager();

        sessionManager.appendUserMessage("session-1", "hello");
        sessionManager.appendAssistantMessage("session-1", "Hi.");

        SessionContext context = sessionManager.loadContext("session-1");

        assertThat(context.history()).containsExactly(
            new ChatMessage(ChatMessage.Role.USER, "hello"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi.")
        );
        assertThat(sessionManager.lastAssistantMessage("session-1")).contains("Hi.");
    }

    @Test
    @DisplayName("세션 이력은 최근 20개 메시지만 유지한다")
    void keeps_recent_twenty_messages() {
        InMemorySessionManager sessionManager = new InMemorySessionManager();

        for (int i = 1; i <= 25; i++) {
            sessionManager.appendUserMessage("session-1", "message-" + i);
        }

        assertThat(sessionManager.loadContext("session-1").history())
            .hasSize(20)
            .first()
            .isEqualTo(new ChatMessage(ChatMessage.Role.USER, "message-6"));
    }
}
