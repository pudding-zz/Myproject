package com.myproject.website.modules.character.dto;

import com.myproject.website.modules.character.entity.CharacterEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CharacterResponse {

    private Long id;
    private Long storyBaseId;
    private String name;
    private String gender;
    private String title;
    private String setting;
    private String personality;
    private String systemPrompt;
    private Boolean playerInsert;
    private Boolean enabled;

    public static CharacterResponse from(CharacterEntity entity) {
        return CharacterResponse.builder()
                .id(entity.getId())
                .storyBaseId(entity.getStoryBaseId())
                .name(entity.getName())
                .gender(entity.getGender())
                .title(entity.getTitle())
                .setting(entity.getSetting())
                .personality(entity.getPersonality())
                .systemPrompt(entity.getSystemPrompt())
                .playerInsert(entity.getPlayerInsert())
                .enabled(entity.getEnabled())
                .build();
    }
}
