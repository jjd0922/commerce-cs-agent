package com.commerce.cs.api.chat;

import com.commerce.cs.application.chat.ChatCommand;
import com.commerce.cs.application.chat.ChatResult;
import com.commerce.cs.application.chat.ChatUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatUseCase chatUseCase;

    public ChatController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResult result = chatUseCase.handle(new ChatCommand(request.sessionId(), request.message()));
        return ResponseEntity.ok(ChatResponseMapper.from(result));
    }
}
