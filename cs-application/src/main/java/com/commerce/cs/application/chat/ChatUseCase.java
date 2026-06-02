package com.commerce.cs.application.chat;

public interface ChatUseCase {

    ChatResult handle(ChatCommand command);
}
