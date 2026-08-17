package com.myproject.website.modules.roleplay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendRoleplayMessageRequest {

    @NotBlank
    private String content;
}
