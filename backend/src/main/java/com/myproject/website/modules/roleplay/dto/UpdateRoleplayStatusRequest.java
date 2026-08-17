package com.myproject.website.modules.roleplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 角色状态全量更新请求；字段与前端 roleplayStatusDemo 对齐。
 * 也接受仅含 payload 键的 Map 式 JSON（多余字段忽略）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRoleplayStatusRequest {

    private List<Map<String, Object>> blocks;
    private List<String> intimacy;
    private List<Map<String, Object>> life;
    private List<Map<String, Object>> favorability;
    private String favorOs;
    private List<Map<String, Object>> forum;
    private Map<String, Object> theater;
    private List<Map<String, Object>> misc;
    private Map<String, Object> access;
}
