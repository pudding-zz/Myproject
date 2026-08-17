package com.myproject.website.modules.roleplay.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RoleplayHealthResponse {

    private boolean available;
    private Map<String, Integer> summary;
    private List<RoleplayHealthRecordDto> records;
}
