package com.myproject.website.modules.roleplay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.config.AiProperties;
import com.myproject.website.modules.ai.AiClient;
import com.myproject.website.modules.ai.AiHistoryWindow;
import com.myproject.website.modules.ai.AiMessage;
import com.myproject.website.modules.roleplay.dto.CreateRoleplaySessionRequest;
import com.myproject.website.modules.roleplay.dto.RoleplayHealthRecordDto;
import com.myproject.website.modules.roleplay.dto.RoleplayHealthResponse;
import com.myproject.website.modules.roleplay.dto.RoleplayMessageResponse;
import com.myproject.website.modules.roleplay.dto.RoleplaySessionResponse;
import com.myproject.website.modules.roleplay.dto.UpdateRoleplayHealthRequest;
import com.myproject.website.modules.roleplay.dto.UpdateRoleplayStatusRequest;
import com.myproject.website.modules.roleplay.entity.RoleplayHealthRecordEntity;
import com.myproject.website.modules.roleplay.entity.RoleplayMessageEntity;
import com.myproject.website.modules.roleplay.entity.RoleplaySessionEntity;
import com.myproject.website.modules.roleplay.entity.RoleplaySessionStatusEntity;
import com.myproject.website.modules.roleplay.repository.RoleplayHealthRecordRepository;
import com.myproject.website.modules.roleplay.repository.RoleplayMessageRepository;
import com.myproject.website.modules.roleplay.repository.RoleplaySessionRepository;
import com.myproject.website.modules.roleplay.repository.RoleplaySessionStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleplayService {

    private static final List<String> STATUS_KEYS = List.of(
            "blocks", "intimacy", "life", "favorability", "favorOs", "forum", "theater", "misc", "access");

    private final RoleplaySessionRepository sessionRepository;
    private final RoleplayMessageRepository messageRepository;
    private final RoleplayHealthRecordRepository healthRecordRepository;
    private final RoleplaySessionStatusRepository statusRepository;
    private final RoleplayPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RoleplaySessionResponse> listSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(RoleplaySessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleplaySessionResponse getSession(Long id) {
        return RoleplaySessionResponse.from(requireSession(id));
    }

    @Transactional
    public RoleplaySessionResponse create(CreateRoleplaySessionRequest request) {
        RoleplaySessionEntity session = new RoleplaySessionEntity();
        session.setUserId(StringUtils.hasText(request.getUserId()) ? request.getUserId() : "local");
        session.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle()
                : request.getAiName() + " × " + request.getPlayerName());
        session.setAiName(request.getAiName().trim());
        session.setAiGender(trimToNull(request.getAiGender()));
        session.setAiTitle(trimToNull(request.getAiTitle()));
        session.setAiPersonality(trimToNull(request.getAiPersonality()));
        session.setAiRelation(trimToNull(request.getAiRelation()));
        session.setPlayerName(request.getPlayerName().trim());
        session.setPlayerGender(trimToNull(request.getPlayerGender()));
        session.setPlayerTitle(trimToNull(request.getPlayerTitle()));
        session.setPlayerPersonality(trimToNull(request.getPlayerPersonality()));
        session.setPlayerRelation(trimToNull(request.getPlayerRelation()));
        session.setScene(trimToNull(request.getScene()));
        sessionRepository.save(session);

        seedEmptyStatus(session.getId());

        if (request.getOpeningLine() == null || Boolean.TRUE.equals(request.getOpeningLine())) {
            generateAssistantReply(session, promptBuilder.buildOpeningUserPrompt(session), false);
        }
        return RoleplaySessionResponse.from(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        requireSession(sessionId);
        healthRecordRepository.deleteBySessionId(sessionId);
        statusRepository.deleteBySessionId(sessionId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    @Transactional(readOnly = true)
    public List<RoleplayMessageResponse> listMessages(Long sessionId) {
        requireSession(sessionId);
        return messageRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .map(RoleplayMessageResponse::from)
                .toList();
    }

    @Transactional
    public RoleplayMessageResponse sendMessage(Long sessionId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }
        RoleplaySessionEntity session = requireSession(sessionId);

        RoleplayMessageEntity userMessage = new RoleplayMessageEntity();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(content.trim());
        messageRepository.save(userMessage);

        generateAssistantReply(
                session, promptBuilder.buildChatUserPrompt(session, content.trim()), true);
        sessionRepository.save(session);

        RoleplayMessageEntity last = messageRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .reduce((a, b) -> b)
                .orElseThrow();
        if (!"assistant".equals(last.getRole())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 回复未写入");
        }
        return RoleplayMessageResponse.from(last);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(Long sessionId) {
        requireSession(sessionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.putAll(loadStatusPayload(sessionId));
        return response;
    }

    @Transactional
    public Map<String, Object> updateStatus(Long sessionId, UpdateRoleplayStatusRequest request) {
        requireSession(sessionId);
        Map<String, Object> payload = toStatusPayload(request);
        saveStatusPayload(sessionId, payload);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.putAll(payload);
        return response;
    }

    /**
     * 「刷新当前状态」：根据最近对话结算并写入 status/health，再返回最新快照。
     */
    @Transactional
    public Map<String, Object> refreshStatus(Long sessionId) {
        RoleplaySessionEntity session = requireSession(sessionId);
        Map<String, Object> current = loadStatusPayload(sessionId);
        String statusJson;
        try {
            statusJson = objectMapper.writeValueAsString(current);
        } catch (Exception e) {
            statusJson = "{}";
        }

        List<RoleplayMessageEntity> recent = AiHistoryWindow.recent(
                messageRepository.findBySessionIdOrderByIdAsc(sessionId),
                aiProperties.getHistoryMaxMessages());
        String transcript = buildTranscript(recent);

        List<AiMessage> prompt = List.of(
                AiMessage.system(promptBuilder.buildStatusSettleSystemPrompt()),
                AiMessage.user(promptBuilder.buildStatusSettleUserPrompt(session, statusJson, transcript)));

        String raw = aiClient.chat(prompt);
        JsonNode root = parseJsonObject(raw);

        boolean changed = root.path("changed").asBoolean(false);
        String note = root.path("note").asText("");
        Map<String, Object> payload = new LinkedHashMap<>(current);

        JsonNode statusNode = root.get("status");
        if (statusNode != null && statusNode.isObject()) {
            mergeStatusPatch(payload, statusNode);
            changed = true;
        }

        JsonNode healthPatch = root.get("healthPatch");
        if (healthPatch != null && !healthPatch.isNull() && healthPatch.isObject()) {
            applyHealthPatch(sessionId, healthPatch);
            changed = true;
        }

        if (changed) {
            saveStatusPayload(sessionId, payload);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.put("changed", changed);
        response.put("note", StringUtils.hasText(note)
                ? note
                : (changed ? "已根据对话整理状态" : "对话中暂无明显状态变化"));
        response.putAll(payload);
        response.put("health", getHealth(sessionId));
        return response;
    }

    @Transactional(readOnly = true)
    public RoleplayHealthResponse getHealth(Long sessionId) {
        requireSession(sessionId);
        List<RoleplayHealthRecordDto> records = healthRecordRepository
                .findBySessionIdOrderByDayAsc(sessionId)
                .stream()
                .map(RoleplayHealthRecordDto::from)
                .toList();
        return RoleplayHealthResponse.builder()
                .available(true)
                .summary(summarize(records))
                .records(records)
                .build();
    }

    @Transactional
    public RoleplayHealthResponse updateHealth(Long sessionId, UpdateRoleplayHealthRequest request) {
        requireSession(sessionId);
        List<RoleplayHealthRecordDto> incoming =
                request.getRecords() == null ? List.of() : request.getRecords();

        Set<Integer> seenDays = new HashSet<>();
        for (RoleplayHealthRecordDto dto : incoming) {
            if (dto.getDay() == null || dto.getDay() < 1 || dto.getDay() > 31) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "day 必须为 1–31");
            }
            if (!seenDays.add(dto.getDay())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "records 中 day 不能重复: " + dto.getDay());
            }
        }

        replaceHealthRecords(sessionId, incoming);
        List<RoleplayHealthRecordDto> records = healthRecordRepository
                .findBySessionIdOrderByDayAsc(sessionId)
                .stream()
                .map(RoleplayHealthRecordDto::from)
                .toList();
        return RoleplayHealthResponse.builder()
                .available(true)
                .summary(summarize(records))
                .records(records)
                .build();
    }

    private void applyHealthPatch(Long sessionId, JsonNode patch) {
        int day = patch.path("day").asInt(0);
        if (day < 1 || day > 31) {
            return;
        }
        List<RoleplayHealthRecordDto> records = healthRecordRepository
                .findBySessionIdOrderByDayAsc(sessionId)
                .stream()
                .map(RoleplayHealthRecordDto::from)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (records.isEmpty()) {
            for (int d = 1; d <= 31; d++) {
                RoleplayHealthRecordDto blank = new RoleplayHealthRecordDto();
                blank.setDay(d);
                blank.setCal(0);
                blank.setHeart(0);
                blank.setCount(0);
                blank.setDuration(0);
                blank.setTrigger("");
                blank.setScene("");
                blank.setThought("");
                records.add(blank);
            }
        }
        boolean found = false;
        for (RoleplayHealthRecordDto r : records) {
            if (r.getDay() != null && r.getDay() == day) {
                if (patch.has("cal")) {
                    r.setCal(patch.path("cal").asInt(0));
                }
                if (patch.has("heart")) {
                    r.setHeart(patch.path("heart").asInt(0));
                }
                if (patch.has("count")) {
                    r.setCount(patch.path("count").asInt(0));
                }
                if (patch.has("duration")) {
                    r.setDuration(patch.path("duration").asInt(0));
                }
                if (patch.has("trigger")) {
                    r.setTrigger(patch.path("trigger").asText(""));
                }
                if (patch.has("scene")) {
                    r.setScene(patch.path("scene").asText(""));
                }
                if (patch.has("thought")) {
                    r.setThought(patch.path("thought").asText(""));
                }
                found = true;
                break;
            }
        }
        if (!found) {
            RoleplayHealthRecordDto dto = new RoleplayHealthRecordDto();
            dto.setDay(day);
            dto.setCal(patch.path("cal").asInt(0));
            dto.setHeart(patch.path("heart").asInt(0));
            dto.setCount(patch.path("count").asInt(0));
            dto.setDuration(patch.path("duration").asInt(0));
            dto.setTrigger(patch.path("trigger").asText(""));
            dto.setScene(patch.path("scene").asText(""));
            dto.setThought(patch.path("thought").asText(""));
            records.add(dto);
        }
        replaceHealthRecords(sessionId, records);
    }

    private void replaceHealthRecords(Long sessionId, List<RoleplayHealthRecordDto> incoming) {
        healthRecordRepository.deleteBySessionId(sessionId);
        healthRecordRepository.flush();
        List<RoleplayHealthRecordEntity> entities = new ArrayList<>();
        for (RoleplayHealthRecordDto dto : incoming) {
            RoleplayHealthRecordEntity entity = new RoleplayHealthRecordEntity();
            entity.setSessionId(sessionId);
            entity.setDay(dto.getDay());
            entity.setCal(nullToZero(dto.getCal()));
            entity.setHeart(nullToZero(dto.getHeart()));
            entity.setCount(nullToZero(dto.getCount()));
            entity.setDuration(nullToZero(dto.getDuration()));
            entity.setTriggerText(dto.getTrigger());
            entity.setScene(dto.getScene());
            entity.setThought(dto.getThought());
            entities.add(entity);
        }
        healthRecordRepository.saveAll(entities);
    }

    @SuppressWarnings("unchecked")
    private void mergeStatusPatch(Map<String, Object> payload, JsonNode statusNode) {
        for (String key : STATUS_KEYS) {
            if (!statusNode.has(key) || statusNode.get(key).isNull()) {
                continue;
            }
            JsonNode value = statusNode.get(key);
            if ("life".equals(key) && value.isArray()) {
                payload.put(key, mergeLifeByTitle(
                        (List<Map<String, Object>>) payload.getOrDefault("life", List.of()),
                        value));
            } else {
                payload.put(key, objectMapper.convertValue(value, Object.class));
            }
        }
    }

    private List<Map<String, Object>> mergeLifeByTitle(
            List<Map<String, Object>> existing, JsonNode patchArray) {
        Map<String, Map<String, Object>> byTitle = new LinkedHashMap<>();
        if (existing != null) {
            for (Map<String, Object> item : existing) {
                Object title = item.get("title");
                if (title != null) {
                    byTitle.put(String.valueOf(title), new LinkedHashMap<>(item));
                }
            }
        }
        for (JsonNode node : patchArray) {
            Map<String, Object> item = objectMapper.convertValue(node, new TypeReference<>() {
            });
            Object title = item.get("title");
            if (title == null) {
                continue;
            }
            byTitle.put(String.valueOf(title), item);
        }
        if (byTitle.isEmpty()) {
            List<Map<String, Object>> fromPatch = objectMapper.convertValue(
                    patchArray, new TypeReference<>() {
                    });
            return fromPatch != null ? fromPatch : List.of();
        }
        // 保证四块顺序
        List<String> order = List.of("进食", "睡眠", "礼物", "约定");
        List<Map<String, Object>> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String title : order) {
            if (byTitle.containsKey(title)) {
                merged.add(byTitle.get(title));
                seen.add(title);
            }
        }
        for (Map.Entry<String, Map<String, Object>> e : byTitle.entrySet()) {
            if (!seen.contains(e.getKey())) {
                merged.add(e.getValue());
            }
        }
        return merged;
    }

    private JsonNode parseJsonObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.AI_ERROR, "状态整理未返回内容");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                text = text.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.AI_ERROR, "状态整理返回不是 JSON");
        }
        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            log.warn("parse status settle json failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_ERROR, "状态整理 JSON 解析失败");
        }
    }

    private Map<String, Object> loadStatusPayload(Long sessionId) {
        return statusRepository.findBySessionId(sessionId)
                .map(RoleplaySessionStatusEntity::getPayload)
                .filter(p -> p != null)
                .map(p -> (Map<String, Object>) new LinkedHashMap<>(p))
                .orElseGet(RoleplayService::emptyStatusPayload);
    }

    private void saveStatusPayload(Long sessionId, Map<String, Object> payload) {
        RoleplaySessionStatusEntity status = statusRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    RoleplaySessionStatusEntity created = new RoleplaySessionStatusEntity();
                    created.setSessionId(sessionId);
                    return created;
                });
        status.setPayload(payload);
        statusRepository.save(status);
    }

    private void seedEmptyStatus(Long sessionId) {
        RoleplaySessionStatusEntity status = new RoleplaySessionStatusEntity();
        status.setSessionId(sessionId);
        status.setPayload(emptyStatusPayload());
        statusRepository.save(status);
    }

    static Map<String, Object> emptyStatusPayload() {
        Map<String, Object> theater = new LinkedHashMap<>();
        theater.put("content", "");
        theater.put("os", "");

        Map<String, Object> access = new LinkedHashMap<>();
        access.put("lines", List.of());
        access.put("os", "");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("blocks", List.of());
        payload.put("intimacy", List.of());
        payload.put("life", List.of());
        payload.put("favorability", List.of());
        payload.put("favorOs", "");
        payload.put("forum", List.of());
        payload.put("theater", theater);
        payload.put("misc", List.of());
        payload.put("access", access);
        return payload;
    }

    private static Map<String, Object> toStatusPayload(UpdateRoleplayStatusRequest request) {
        Map<String, Object> payload = emptyStatusPayload();
        if (request == null) {
            return payload;
        }
        if (request.getBlocks() != null) {
            payload.put("blocks", request.getBlocks());
        }
        if (request.getIntimacy() != null) {
            payload.put("intimacy", request.getIntimacy());
        }
        if (request.getLife() != null) {
            payload.put("life", request.getLife());
        }
        if (request.getFavorability() != null) {
            payload.put("favorability", request.getFavorability());
        }
        if (request.getFavorOs() != null) {
            payload.put("favorOs", request.getFavorOs());
        }
        if (request.getForum() != null) {
            payload.put("forum", request.getForum());
        }
        if (request.getTheater() != null) {
            payload.put("theater", request.getTheater());
        }
        if (request.getMisc() != null) {
            payload.put("misc", request.getMisc());
        }
        if (request.getAccess() != null) {
            payload.put("access", request.getAccess());
        }
        return payload;
    }

    private static Map<String, Integer> summarize(List<RoleplayHealthRecordDto> records) {
        int totalCount = 0;
        int totalCal = 0;
        int heartSum = 0;
        int heartDays = 0;
        for (RoleplayHealthRecordDto r : records) {
            int count = nullToZero(r.getCount());
            totalCount += count;
            totalCal += nullToZero(r.getCal());
            if (count > 0) {
                heartSum += nullToZero(r.getHeart());
                heartDays++;
            }
        }
        int avgHeart = heartDays == 0 ? 0 : Math.round((float) heartSum / heartDays);
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalCal", totalCal);
        summary.put("avgHeart", avgHeart);
        return summary;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildTranscript(List<RoleplayMessageEntity> messages) {
        StringBuilder sb = new StringBuilder();
        for (RoleplayMessageEntity msg : messages) {
            String who = "user".equals(msg.getRole()) ? "玩家" : "AI";
            sb.append(who).append("：").append(msg.getContent()).append('\n');
        }
        return sb.toString().trim();
    }

    private String generateAssistantReply(
            RoleplaySessionEntity session, String latestUserPrompt, boolean includeHistory) {
        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(AiMessage.system(promptBuilder.buildSystemPrompt(session)));

        if (includeHistory) {
            List<RoleplayMessageEntity> history = AiHistoryWindow.recent(
                    messageRepository.findBySessionIdOrderByIdAsc(session.getId()),
                    aiProperties.getHistoryMaxMessages());
            for (int i = 0; i < history.size(); i++) {
                RoleplayMessageEntity msg = history.get(i);
                boolean isLast = i == history.size() - 1;
                if ("user".equals(msg.getRole())) {
                    if (isLast) {
                        prompt.add(AiMessage.user(latestUserPrompt));
                    } else {
                        prompt.add(AiMessage.user(
                                promptBuilder.buildChatUserPrompt(session, msg.getContent())));
                    }
                } else if ("assistant".equals(msg.getRole())) {
                    prompt.add(AiMessage.assistant(msg.getContent()));
                }
            }
        } else {
            prompt.add(AiMessage.user(latestUserPrompt));
        }

        String reply = aiClient.chat(prompt);
        if (!StringUtils.hasText(reply)) {
            reply = "……";
        }

        RoleplayMessageEntity assistantMessage = new RoleplayMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply.trim());
        messageRepository.save(assistantMessage);
        return reply.trim();
    }

    private RoleplaySessionEntity requireSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色扮演会话不存在"));
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
