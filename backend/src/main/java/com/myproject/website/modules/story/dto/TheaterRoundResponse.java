package com.myproject.website.modules.story.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TheaterRoundResponse {

    private List<TheaterLine> lines;
    private String worldSummary;
    private String divergence;

    @Data
    @Builder
    public static class TheaterLine {
        private Long characterId;
        private String characterName;
        private String content;
    }
}
