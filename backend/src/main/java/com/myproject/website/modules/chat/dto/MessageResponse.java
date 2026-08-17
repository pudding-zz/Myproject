package com.myproject.website.modules.chat.dto;

import com.myproject.website.modules.chat.entity.ChatMessageEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long chatId;
    private String role;
    private String content;
    private Instant createdAt;
    /** 偏离记录摘要（推进剧情时可能返回） */
    private String divergence;
    private String worldSummary;

    public static MessageResponse from(ChatMessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .chatId(entity.getChatId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
