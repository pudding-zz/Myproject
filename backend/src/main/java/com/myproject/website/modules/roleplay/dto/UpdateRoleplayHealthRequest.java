package com.myproject.website.modules.roleplay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRoleplayHealthRequest {

    @NotNull
    @Valid
    private List<RoleplayHealthRecordDto> records;
}
