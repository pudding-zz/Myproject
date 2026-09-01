package com.myproject.website.modules.roleplay.controller;

import com.myproject.website.common.ApiResponse;
import com.myproject.website.modules.roleplay.dto.CreateRoleplaySessionRequest;
import com.myproject.website.modules.roleplay.dto.RoleplayHealthResponse;
import com.myproject.website.modules.roleplay.dto.RoleplayMessageResponse;
import com.myproject.website.modules.roleplay.dto.RoleplaySessionResponse;
import com.myproject.website.modules.roleplay.dto.SendRoleplayMessageRequest;
import com.myproject.website.modules.roleplay.dto.UpdateRoleplayHealthRequest;
import com.myproject.website.modules.roleplay.dto.UpdateRoleplayStatusRequest;
import com.myproject.website.modules.roleplay.service.RoleplayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/roleplay")
@RequiredArgsConstructor
public class RoleplayController {

    private final RoleplayService roleplayService;

    @GetMapping("/sessions")
    public ApiResponse<List<RoleplaySessionResponse>> list() {
        return ApiResponse.ok(roleplayService.listSessions());
    }

    @PostMapping("/sessions")
    public ApiResponse<RoleplaySessionResponse> create(
            @Valid @RequestBody CreateRoleplaySessionRequest request) {
        return ApiResponse.ok(roleplayService.create(request));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<RoleplaySessionResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(roleplayService.getSession(id));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleplayService.deleteSession(id);
        return ApiResponse.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<RoleplayMessageResponse>> messages(@PathVariable Long id) {
        return ApiResponse.ok(roleplayService.listMessages(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<RoleplayMessageResponse> send(
            @PathVariable Long id,
            @Valid @RequestBody SendRoleplayMessageRequest request) {
        return ApiResponse.ok(roleplayService.sendMessage(id, request.getContent()));
    }

    @GetMapping("/sessions/{id}/status")
    public ApiResponse<Map<String, Object>> status(@PathVariable Long id) {
        return ApiResponse.ok(roleplayService.getStatus(id));
    }

    @PutMapping("/sessions/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateRoleplayStatusRequest request) {
        return ApiResponse.ok(roleplayService.updateStatus(id, request));
    }

    /** 根据最近对话整理并写入状态（对用户表现为「刷新当前状态」） */
    @PostMapping("/sessions/{id}/status/refresh")
    public ApiResponse<Map<String, Object>> refreshStatus(@PathVariable Long id) {
        return ApiResponse.ok(roleplayService.refreshStatus(id));
    }

    @GetMapping("/sessions/{id}/health")
    public ApiResponse<RoleplayHealthResponse> health(@PathVariable Long id) {
        return ApiResponse.ok(roleplayService.getHealth(id));
    }

    @PutMapping("/sessions/{id}/health")
    public ApiResponse<RoleplayHealthResponse> updateHealth(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleplayHealthRequest request) {
        return ApiResponse.ok(roleplayService.updateHealth(id, request));
    }
}
