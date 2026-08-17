package com.myproject.website.modules.roleplay.dto;

import com.myproject.website.modules.roleplay.entity.RoleplaySessionEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RoleplaySessionResponse {

    private Long id;
    private String userId;
    private String title;
    private String aiName;
    private String aiGender;
    private String aiTitle;
    private String aiPersonality;
    private String aiRelation;
    private String playerName;
    private String playerGender;
    private String playerTitle;
    private String playerPersonality;
    private String playerRelation;
    private String scene;
    private Instant createdAt;
    private Instant updatedAt;

    public static RoleplaySessionResponse from(RoleplaySessionEntity s) {
        return RoleplaySessionResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .title(s.getTitle())
                .aiName(s.getAiName())
                .aiGender(s.getAiGender())
                .aiTitle(s.getAiTitle())
                .aiPersonality(s.getAiPersonality())
                .aiRelation(s.getAiRelation())
                .playerName(s.getPlayerName())
                .playerGender(s.getPlayerGender())
                .playerTitle(s.getPlayerTitle())
                .playerPersonality(s.getPlayerPersonality())
                .playerRelation(s.getPlayerRelation())
                .scene(s.getScene())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
