package com.myproject.website.modules.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCharacterRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 16)
    private String gender;

    @Size(max = 128)
    private String title;

    @Size(max = 2000)
    private String setting;

    @Size(max = 2000)
    private String personality;

    private Boolean playerInsert;
}
