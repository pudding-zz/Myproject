package com.myproject.website.modules.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateChatResponse {

    private Long id;
    private Long characterId;
    private Long storyBaseId;
    private String userId;
}
