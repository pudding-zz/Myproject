package com.myproject.website.modules.story.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FromTitleRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String author;
}
