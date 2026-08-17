package com.myproject.website.modules.roleplay.service;

import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.modules.ai.AiClient;
import com.myproject.website.modules.ai.AiMessage;
import com.myproject.website.modules.roleplay.dto.CreateRoleplaySessionRequest;
import com.myproject.website.modules.roleplay.dto.RoleplayMessageResponse;
import com.myproject.website.modules.roleplay.dto.RoleplaySessionResponse;
import com.myproject.website.modules.roleplay.entity.RoleplayMessageEntity;
import com.myproject.website.modules.roleplay.entity.RoleplaySessionEntity;
import com.myproject.website.modules.roleplay.repository.RoleplayMessageRepository;
import com.myproject.website.modules.roleplay.repository.RoleplaySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleplayService {

    private final RoleplaySessionRepository sessionRepository;
    private final RoleplayMessageRepository messageRepository;
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

        String reply = generateAssistantReply(
                session, promptBuilder.buildChatUserPrompt(session, content.trim()), true);
        // touch updatedAt
        sessionRepository.save(session);

        RoleplayMessageEntity last = messageRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .reduce((a, b) -> b)
                .orElseThrow();
        if (!"assistant".equals(last.getRole())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 回复未写入");
        }
        return RoleplayMessageResponse.from(last);
    }

    /**
     * 后续扩展占位：角色状态 / 生理记录 / 亲密记录尚未落库，先返回空结构。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatusPlaceholder(Long sessionId) {
        requireSession(sessionId);
        return Map.of(
                "available", false,
                "message", "角色状态将在后续版本接入",
                "blocks", List.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHealthPlaceholder(Long sessionId) {
        requireSession(sessionId);
        return Map.of(
                "available", false,
                "message", "生理/亲密记录将在后续版本接入",
                "summary", Map.of(
                        "totalCount", 0,
                        "totalCal", 0,
                        "avgHeart", 0),
                "records", List.of());
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
