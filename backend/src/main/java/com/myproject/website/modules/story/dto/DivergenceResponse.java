package com.myproject.website.modules.story.dto;

import com.myproject.website.modules.story.entity.DivergenceLogEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DivergenceResponse {

    private Long id;
    private Long storyBaseId;
    private Long canonNodeId;
    private String originalText;
    private String newText;
    private Instant createdAt;

    public static DivergenceResponse from(DivergenceLogEntity e) {
        return DivergenceResponse.builder()
                .id(e.getId())
                .storyBaseId(e.getStoryBaseId())
                .canonNodeId(e.getCanonNodeId())
                .originalText(e.getOriginalText())
                .newText(e.getNewText())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
