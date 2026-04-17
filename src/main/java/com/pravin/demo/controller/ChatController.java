package com.pravin.demo.controller;

import com.pravin.demo.model.ChatRequest;
import com.pravin.demo.model.ChatResponse;
import com.pravin.demo.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.question());
        return new ChatResponse(answer);
    }
}