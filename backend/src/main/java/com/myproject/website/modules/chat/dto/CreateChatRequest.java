package com.myproject.website.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatRequest {

    @NotNull(message = "characterId is required")
    private Long characterId;

    private Long storyBaseId;

    private String userId;
}
