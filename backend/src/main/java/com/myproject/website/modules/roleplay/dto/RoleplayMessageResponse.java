package com.myproject.website.modules.roleplay.dto;

import com.myproject.website.modules.roleplay.entity.RoleplayMessageEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RoleplayMessageResponse {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Instant createdAt;

    public static RoleplayMessageResponse from(RoleplayMessageEntity m) {
        return RoleplayMessageResponse.builder()
                .id(m.getId())
                .sessionId(m.getSessionId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
