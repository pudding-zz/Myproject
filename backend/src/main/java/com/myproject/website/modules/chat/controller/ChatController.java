package com.myproject.website.modules.chat.controller;

import com.myproject.website.common.ApiResponse;
import com.myproject.website.modules.chat.dto.CreateChatRequest;
import com.myproject.website.modules.chat.dto.CreateChatResponse;
import com.myproject.website.modules.chat.dto.MessageResponse;
import com.myproject.website.modules.chat.dto.SendMessageRequest;
import com.myproject.website.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ApiResponse<CreateChatResponse> create(@Valid @RequestBody CreateChatRequest request) {
        return ApiResponse.ok(chatService.create(request));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageResponse>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageResponse> send(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(id, request.getContent()));
    }

    @PostMapping("/{id}/advance")
    public ApiResponse<MessageResponse> advance(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String content = body == null ? null : body.get("content");
        return ApiResponse.ok(chatService.advance(id, content));
    }
}
