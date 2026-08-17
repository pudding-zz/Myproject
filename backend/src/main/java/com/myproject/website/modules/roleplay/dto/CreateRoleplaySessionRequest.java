package com.myproject.website.modules.roleplay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleplaySessionRequest {

    private String userId;

    /** 会话展示标题，可选 */
    private String title;

    @NotBlank
    private String aiName;
    private String aiGender;
    private String aiTitle;
    private String aiPersonality;
    private String aiRelation;

    @NotBlank
    private String playerName;
    private String playerGender;
    private String playerTitle;
    private String playerPersonality;
    private String playerRelation;

    /** 开场场景/氛围，可选 */
    private String scene;

    /** 是否让 AI 先说开场白，默认 true */
    private Boolean openingLine = true;
}
