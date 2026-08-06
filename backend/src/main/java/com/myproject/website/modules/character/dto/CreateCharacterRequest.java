package com.myproject.website.modules.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCharacterRequest {

    @NotNull(message = "storyBaseId is required")
    private Long storyBaseId;

    @NotBlank(message = "name is required")
    private String name;

    /** male / female / other */
    private String gender = "male";

    private String title;

    private String setting;

    private String personality;

    private Boolean playerInsert = false;
}
