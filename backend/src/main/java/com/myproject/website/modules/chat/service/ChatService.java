package com.myproject.website.modules.chat.service;

import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.config.AiProperties;
import com.myproject.website.modules.ai.AiClient;
import com.myproject.website.modules.ai.AiHistoryWindow;
import com.myproject.website.modules.ai.AiMessage;
import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.character.service.CharacterService;
import com.myproject.website.modules.chat.dto.CreateChatRequest;
import com.myproject.website.modules.chat.dto.CreateChatResponse;
import com.myproject.website.modules.chat.dto.MessageResponse;
import com.myproject.website.modules.chat.entity.ChatMessageEntity;
import com.myproject.website.modules.chat.entity.ChatSessionEntity;
import com.myproject.website.modules.chat.repository.ChatMessageRepository;
import com.myproject.website.modules.chat.repository.ChatSessionRepository;
import com.myproject.website.modules.story.service.StoryPromptBuilder;
import com.myproject.website.modules.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Pattern DIVERGENCE = Pattern.compile("(?m)^DIVERGENCE:\\s*(.+)$");
    private static final Pattern WORLD = Pattern.compile("(?m)^WORLD:\\s*(.+)$");

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CharacterService characterService;
    private final StoryService storyService;
    private final StoryPromptBuilder storyPromptBuilder;
    private final AiClient aiClient;
    private final AiProperties aiProperties;

    @Transactional
    public CreateChatResponse create(CreateChatRequest request) {
        CharacterEntity character = characterService.requireEnabled(request.getCharacterId());
        Long storyBaseId = request.getStoryBaseId() != null
                ? request.getStoryBaseId()
                : character.getStoryBaseId();
        if (storyBaseId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话需要关联剧情底本");
        }
        storyService.requireConfirmed(storyBaseId);
        if (character.getStoryBaseId() != null && !character.getStoryBaseId().equals(storyBaseId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不属于该剧情底本");
        }

        ChatSessionEntity session = new ChatSessionEntity();
        session.setCharacterId(character.getId());
        session.setStoryBaseId(storyBaseId);
        session.setUserId(StringUtils.hasText(request.getUserId()) ? request.getUserId() : "local");
        chatSessionRepository.save(session);

        return CreateChatResponse.builder()
                .id(session.getId())
                .characterId(session.getCharacterId())
                .storyBaseId(session.getStoryBaseId())
                .userId(session.getUserId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(Long chatId) {
        requireSession(chatId);
        return chatMessageRepository.findByChatIdOrderByIdAsc(chatId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long chatId, String content) {
        return converse(chatId, content, false);
    }

    @Transactional
    public MessageResponse advance(Long chatId, String content) {
        String action = StringUtils.hasText(content) ? content : "（推进剧情）";
        return converse(chatId, action, true);
    }

    private MessageResponse converse(Long chatId, String content, boolean advance) {
        ChatSessionEntity session = requireSession(chatId);
        CharacterEntity character = characterService.requireEnabled(session.getCharacterId());
        Long storyBaseId = session.getStoryBaseId();

        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setChatId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        chatMessageRepository.save(userMessage);

        String storyContext = storyService.buildStoryContext(storyBaseId);
        String system = storyPromptBuilder.buildPlayerPerspectiveSystemPrompt(character, storyContext);

        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(AiMessage.system(system));
        List<ChatMessageEntity> history = AiHistoryWindow.recent(
                chatMessageRepository.findByChatIdOrderByIdAsc(chatId),
                aiProperties.getHistoryMaxMessages());
        history.forEach(msg -> {
            if ("user".equals(msg.getRole()) && msg.getId().equals(userMessage.getId())) {
                if (advance) {
                    prompt.add(AiMessage.user(
                            storyPromptBuilder.buildAdvanceUserPrompt(character.getName(), content)));
                } else {
                    prompt.add(AiMessage.user(
                            storyPromptBuilder.buildChatUserPrompt(character.getName(), content)));
                }
            } else if ("user".equals(msg.getRole())) {
                prompt.add(AiMessage.user(
                        storyPromptBuilder.buildChatUserPrompt(character.getName(), msg.getContent())));
            } else {
                prompt.add(new AiMessage(msg.getRole(), msg.getContent()));
            }
        });

        String rawReply = aiClient.chat(prompt);
        ParsedReply parsed = parseReply(rawReply);

        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setChatId(chatId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(parsed.displayContent());
        chatMessageRepository.save(assistantMessage);

        String divergenceText = null;
        String worldSummary = null;
        if (advance || StringUtils.hasText(parsed.divergence()) || StringUtils.hasText(parsed.worldLine())) {
            if (StringUtils.hasText(parsed.divergence()) && !"无".equals(parsed.divergence().trim())) {
                StoryService.DivergenceApplyResult applied = storyService.applyDivergenceFromAi(
                        storyBaseId, parsed.divergence().trim());
                divergenceText = applied.newText();
            }
            if (StringUtils.hasText(parsed.worldLine())) {
                WorldPatch patch = parseWorldLine(parsed.worldLine());
                storyService.applyWorldUpdate(
                        storyBaseId, patch.time(), patch.place(), patch.present(), patch.summary());
                worldSummary = patch.summary();
            }
        }

        MessageResponse response = MessageResponse.from(assistantMessage);
        response.setDivergence(divergenceText);
        response.setWorldSummary(worldSummary);
        return response;
    }

    static ParsedReply parseReply(String raw) {
        String divergence = null;
        String worldLine = null;
        String content = raw == null ? "" : raw.trim();

        Matcher d = DIVERGENCE.matcher(content);
        if (d.find()) {
            divergence = d.group(1).trim();
            content = d.replaceFirst("").trim();
        }
        Matcher w = WORLD.matcher(content);
        if (w.find()) {
            worldLine = w.group(1).trim();
            content = w.replaceFirst("").trim();
        }
        return new ParsedReply(content, divergence, worldLine);
    }

    static WorldPatch parseWorldLine(String line) {
        String time = null;
        String place = null;
        String present = null;
        String summary = null;
        for (String part : line.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String val = kv[1].trim();
            if (key.contains("时间")) {
                time = val;
            } else if (key.contains("地点")) {
                place = val;
            } else if (key.contains("在场")) {
                present = val;
            } else if (key.contains("摘要")) {
                summary = val;
            }
        }
        if (summary == null) {
            summary = line;
        }
        return new WorldPatch(time, place, present, summary);
    }

    private ChatSessionEntity requireSession(Long chatId) {
        return chatSessionRepository.findById(chatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "chat not found"));
    }

    record ParsedReply(String displayContent, String divergence, String worldLine) {
    }

    record WorldPatch(String time, String place, String present, String summary) {
    }
}
