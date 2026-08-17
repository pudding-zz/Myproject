package com.myproject.website.modules.roleplay.service;

import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.modules.ai.AiClient;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleplayService {

    private final RoleplaySessionRepository sessionRepository;
    private final RoleplayMessageRepository messageRepository;
    private final RoleplayHealthRecordRepository healthRecordRepository;
    private final RoleplaySessionStatusRepository statusRepository;
    private final RoleplayPromptBuilder promptBuilder;
    private final AiClient aiClient;

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
        RoleplaySessionStatusEntity status = statusRepository.findBySessionId(sessionId)
                .orElse(null);
        Map<String, Object> payload = status != null && status.getPayload() != null
                ? status.getPayload()
                : emptyStatusPayload();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.putAll(payload);
        return response;
    }

    @Transactional
    public Map<String, Object> updateStatus(Long sessionId, UpdateRoleplayStatusRequest request) {
        requireSession(sessionId);
        Map<String, Object> payload = toStatusPayload(request);
        RoleplaySessionStatusEntity status = statusRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    RoleplaySessionStatusEntity created = new RoleplaySessionStatusEntity();
                    created.setSessionId(sessionId);
                    return created;
                });
        status.setPayload(payload);
        statusRepository.save(status);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", true);
        response.putAll(payload);
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

        List<RoleplayHealthRecordDto> records = entities.stream()
                .sorted((a, b) -> Integer.compare(a.getDay(), b.getDay()))
                .map(RoleplayHealthRecordDto::from)
                .toList();
        return RoleplayHealthResponse.builder()
                .available(true)
                .summary(summarize(records))
                .records(records)
                .build();
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

    private String generateAssistantReply(
            RoleplaySessionEntity session, String latestUserPrompt, boolean includeHistory) {
        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(AiMessage.system(promptBuilder.buildSystemPrompt(session)));

        if (includeHistory) {
            List<RoleplayMessageEntity> history =
                    messageRepository.findBySessionIdOrderByIdAsc(session.getId());
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
