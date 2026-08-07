package com.myproject.website.modules.story.service;

import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.config.StoryProperties;
import com.myproject.website.modules.ai.AiClient;
import com.myproject.website.modules.ai.AiMessage;
import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.character.repository.CharacterRepository;
import com.myproject.website.modules.story.dto.FromTitleRequest;
import com.myproject.website.modules.story.dto.OutlineJsonParser;
import com.myproject.website.modules.story.dto.OutlineJsonParser.ParsedNode;
import com.myproject.website.modules.story.dto.OutlineJsonParser.ParsedOutline;
import com.myproject.website.modules.story.dto.StoryBaseResponse;
import com.myproject.website.modules.story.dto.TheaterRoundRequest;
import com.myproject.website.modules.story.dto.TheaterRoundResponse;
import com.myproject.website.modules.story.dto.UpsertStoryBaseRequest;
import com.myproject.website.modules.story.dto.DivergenceResponse;
import com.myproject.website.modules.story.entity.CanonNodeEntity;
import com.myproject.website.modules.story.entity.DivergenceLogEntity;
import com.myproject.website.modules.story.entity.StoryBaseEntity;
import com.myproject.website.modules.story.entity.WorldStateEntity;
import com.myproject.website.modules.story.repository.CanonNodeRepository;
import com.myproject.website.modules.story.repository.DivergenceLogRepository;
import com.myproject.website.modules.story.repository.StoryBaseRepository;
import com.myproject.website.modules.story.repository.WorldStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryProperties storyProperties;
    private final StoryBaseRepository storyBaseRepository;
    private final CanonNodeRepository canonNodeRepository;
    private final WorldStateRepository worldStateRepository;
    private final DivergenceLogRepository divergenceLogRepository;
    private final CharacterRepository characterRepository;
    private final AiClient aiClient;
    private final OutlineJsonParser outlineJsonParser;
    private final StoryPromptBuilder storyPromptBuilder;

    @Transactional(readOnly = true)
    public boolean isOutlineFromTitleEnabled() {
        return storyProperties.isOutlineFromTitleEnabled();
    }

    @Transactional(readOnly = true)
    public List<StoryBaseResponse> list() {
        return storyBaseRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryBaseResponse get(Long id) {
        return toResponse(requireBase(id));
    }

    @Transactional
    public StoryBaseResponse createFromTitle(FromTitleRequest request) {
        if (!storyProperties.isOutlineFromTitleEnabled()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "自动取纲已关闭，请改用粘贴/编辑剧情底本");
        }

        String prompt = """
                你是剧情整理助手。根据常见公开情节知识，为小说《%s》%s整理一份「剧情底本」粗纲。
                要求：
                1. 不要输出全文，只要结构化大纲；
                2. 若不确定，合理概括并在 background 中注明可能不准；
                3. 严格只输出 JSON，不要 markdown，格式如下：
                {
                  "title": "书名",
                  "author": "作者或空",
                  "background": "时代/世界观一两段",
                  "nodes": [
                    {
                      "seqNo": 1,
                      "timeLabel": "时间或阶段",
                      "place": "地点",
                      "originalPlot": "该节点原著走向（一两句）"
                    }
                  ]
                }
                nodes 控制在 5 到 10 个关键大事件即可。
                """.formatted(
                request.getTitle(),
                StringUtils.hasText(request.getAuthor()) ? "（作者：" + request.getAuthor() + "）" : "");

        String raw = aiClient.chat(List.of(
                AiMessage.system("你只输出合法 JSON。内容为非官方剧情底本，供私人娱乐。"),
                AiMessage.user(prompt)));

        ParsedOutline outline = outlineJsonParser.parse(raw);
        if (!StringUtils.hasText(outline.getTitle())) {
            outline.setTitle(request.getTitle());
        }
        if (!StringUtils.hasText(outline.getAuthor()) && StringUtils.hasText(request.getAuthor())) {
            outline.setAuthor(request.getAuthor());
        }

        StoryBaseEntity base = new StoryBaseEntity();
        base.setTitle(outline.getTitle());
        base.setAuthor(outline.getAuthor());
        base.setBackground(outline.getBackground());
        base.setStatus("DRAFT");
        storyBaseRepository.save(base);

        saveNodes(base.getId(), outline.getNodes());
        return toResponse(base);
    }

    @Transactional
    public StoryBaseResponse createFromPaste(UpsertStoryBaseRequest request) {
        StoryBaseEntity base = new StoryBaseEntity();
        applyUpsert(base, request);
        storyBaseRepository.save(base);
        replaceNodes(base.getId(), request.getNodes());
        if (request.isConfirm()) {
            confirmInternal(base);
        }
        return toResponse(base);
    }

    @Transactional
    public StoryBaseResponse update(Long id, UpsertStoryBaseRequest request) {
        StoryBaseEntity base = requireBase(id);
        applyUpsert(base, request);
        storyBaseRepository.save(base);
        replaceNodes(id, request.getNodes());
        if (request.isConfirm()) {
            confirmInternal(base);
        }
        return toResponse(base);
    }

    @Transactional(readOnly = true)
    public List<DivergenceResponse> listDivergences(Long storyBaseId) {
        requireBase(storyBaseId);
        return divergenceLogRepository.findByStoryBaseIdOrderByIdDesc(storyBaseId).stream()
                .map(DivergenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryBaseEntity requireBase(Long id) {
        return storyBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "剧情底本不存在"));
    }

    @Transactional(readOnly = true)
    public StoryBaseEntity requireConfirmed(Long id) {
        StoryBaseEntity base = requireBase(id);
        if (!"CONFIRMED".equals(base.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先确认剧情底本再开始穿书");
        }
        return base;
    }

    @Transactional(readOnly = true)
    public String buildStoryContext(Long storyBaseId) {
        StoryBaseEntity base = requireBase(storyBaseId);
        List<CanonNodeEntity> nodes = canonNodeRepository.findByStoryBaseIdOrderBySeqNoAsc(storyBaseId);
        WorldStateEntity world = worldStateRepository.findByStoryBaseId(storyBaseId).orElse(null);
        List<DivergenceLogEntity> recent = divergenceLogRepository
                .findByStoryBaseIdOrderByIdDesc(storyBaseId)
                .stream()
                .limit(8)
                .toList();
        return storyPromptBuilder.buildContext(base, nodes, world, recent);
    }

    @Transactional
    public DivergenceLogEntity recordDivergence(
            Long storyBaseId,
            Long canonNodeId,
            String originalText,
            String newText) {
        DivergenceLogEntity log = new DivergenceLogEntity();
        log.setStoryBaseId(storyBaseId);
        log.setCanonNodeId(canonNodeId);
        log.setOriginalText(originalText);
        log.setNewText(newText);
        return divergenceLogRepository.save(log);
    }

    /**
     * 解析 AI 偏离文本：写日志、更新最近 PENDING 节点为 CHANGED 或 SKIPPED。
     */
    @Transactional
    public DivergenceApplyResult applyDivergenceFromAi(Long storyBaseId, String divergenceText) {
        CanonNodeEntity node = canonNodeRepository.findByStoryBaseIdOrderBySeqNoAsc(storyBaseId).stream()
                .filter(n -> "PENDING".equals(n.getStatus()))
                .findFirst()
                .orElse(null);
        Long nodeId = node == null ? null : node.getId();
        String original = node == null ? null : node.getOriginalPlot();
        DivergenceLogEntity log = recordDivergence(storyBaseId, nodeId, original, divergenceText);
        if (node != null) {
            boolean skipped = looksLikeSkipped(divergenceText);
            node.setStatus(skipped ? "SKIPPED" : "CHANGED");
            node.setChangedPlot(divergenceText);
            canonNodeRepository.save(node);
        }
        return new DivergenceApplyResult(log.getNewText(), node == null ? null : node.getStatus());
    }

    @Transactional
    public void applyWorldUpdate(
            Long storyBaseId,
            String currentTime,
            String currentPlace,
            String presentCharacters,
            String summary) {
        WorldStateEntity world = worldStateRepository.findByStoryBaseId(storyBaseId)
                .orElseGet(() -> {
                    WorldStateEntity w = new WorldStateEntity();
                    w.setStoryBaseId(storyBaseId);
                    return w;
                });
        if (StringUtils.hasText(currentTime)) {
            world.setCurrentTime(currentTime);
        }
        if (StringUtils.hasText(currentPlace)) {
            world.setCurrentPlace(currentPlace);
        }
        if (StringUtils.hasText(presentCharacters)) {
            world.setPresentCharacters(presentCharacters);
        }
        if (StringUtils.hasText(summary)) {
            world.setSummary(summary);
        }
        worldStateRepository.save(world);
    }

    public record DivergenceApplyResult(String newText, String nodeStatus) {
    }

    private static boolean looksLikeSkipped(String divergence) {
        String t = divergence == null ? "" : divergence;
        return t.contains("跳过") || t.contains("未发生") || t.contains("没有发生") || t.contains("取消");
    }

    @Transactional
    public TheaterRoundResponse theaterRound(Long storyBaseId, TheaterRoundRequest request) {
        requireConfirmed(storyBaseId);
        List<CharacterEntity> cast = characterRepository.findAllById(request.getCharacterIds());
        if (cast.size() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对戏至少需要 2 个已创建角色");
        }
        Map<Long, CharacterEntity> byId = cast.stream()
                .collect(Collectors.toMap(CharacterEntity::getId, Function.identity()));
        for (Long id : request.getCharacterIds()) {
            if (!byId.containsKey(id)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在: " + id);
            }
            CharacterEntity c = byId.get(id);
            if (c.getStoryBaseId() == null || !c.getStoryBaseId().equals(storyBaseId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不属于该剧情底本: " + c.getName());
            }
        }

        String context = buildStoryContext(storyBaseId);
        int turns = request.getTurns() == null ? 3 : Math.min(6, Math.max(1, request.getTurns()));
        List<TheaterRoundResponse.TheaterLine> lines = new ArrayList<>();

        if (StringUtils.hasText(request.getPlayerLine())) {
            lines.add(TheaterRoundResponse.TheaterLine.builder()
                    .characterId(null)
                    .characterName("玩家视角")
                    .content(request.getPlayerLine().trim())
                    .build());
        }

        StringBuilder transcript = new StringBuilder();
        for (TheaterRoundResponse.TheaterLine line : lines) {
            transcript.append(line.getCharacterName()).append("：").append(line.getContent()).append('\n');
        }

        List<CharacterEntity> ordered = request.getCharacterIds().stream().map(byId::get).toList();
        for (int i = 0; i < turns; i++) {
            CharacterEntity speaker = ordered.get(i % ordered.size());
            String system = storyPromptBuilder.buildTheaterSpeakerPrompt(speaker, context, transcript.toString());
            String content = aiClient.chat(List.of(
                    AiMessage.system(system),
                    AiMessage.user("请以该角色身份说下一句或一小段（50-120字），推动场景，不要输出角色名冒号前缀。")));
            TheaterRoundResponse.TheaterLine line = TheaterRoundResponse.TheaterLine.builder()
                    .characterId(speaker.getId())
                    .characterName(speaker.getName())
                    .content(content.trim())
                    .build();
            lines.add(line);
            transcript.append(speaker.getName()).append("：").append(content.trim()).append('\n');
        }

        String settleRaw = aiClient.chat(List.of(
                AiMessage.system("你只输出 DIVERGENCE 与 WORLD 两行，用于穿书结算。"),
                AiMessage.user(storyPromptBuilder.buildTheaterDirectorSettlePrompt(
                        context, transcript.toString()))));
        String divergenceText = null;
        String worldSummary = null;
        Matcher d = Pattern.compile("(?m)^DIVERGENCE:\\s*(.+)$").matcher(settleRaw == null ? "" : settleRaw);
        Matcher w = Pattern.compile("(?m)^WORLD:\\s*(.+)$").matcher(settleRaw == null ? "" : settleRaw);
        if (d.find() && StringUtils.hasText(d.group(1)) && !"无".equals(d.group(1).trim())) {
            divergenceText = applyDivergenceFromAi(storyBaseId, d.group(1).trim()).newText();
        }
        if (w.find() && StringUtils.hasText(w.group(1))) {
            String worldLine = w.group(1).trim();
            String time = null;
            String place = null;
            String present = null;
            String summary = null;
            for (String part : worldLine.split(";")) {
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
                summary = worldLine;
            }
            applyWorldUpdate(storyBaseId, time, place, present, summary);
            worldSummary = summary;
        }
        if (worldSummary == null) {
            WorldStateEntity world = worldStateRepository.findByStoryBaseId(storyBaseId).orElse(null);
            worldSummary = world == null ? null : world.getSummary();
        }

        return TheaterRoundResponse.builder()
                .lines(lines)
                .worldSummary(worldSummary)
                .divergence(divergenceText)
                .build();
    }

    private void applyUpsert(StoryBaseEntity base, UpsertStoryBaseRequest request) {
        base.setTitle(request.getTitle());
        base.setAuthor(request.getAuthor());
        base.setBackground(request.getBackground());
    }

    private void confirmInternal(StoryBaseEntity base) {
        base.setStatus("CONFIRMED");
        storyBaseRepository.save(base);
        List<CanonNodeEntity> nodes = canonNodeRepository.findByStoryBaseIdOrderBySeqNoAsc(base.getId());
        WorldStateEntity world = worldStateRepository.findByStoryBaseId(base.getId())
                .orElseGet(() -> {
                    WorldStateEntity w = new WorldStateEntity();
                    w.setStoryBaseId(base.getId());
                    return w;
                });
        if (!nodes.isEmpty()) {
            CanonNodeEntity first = nodes.get(0);
            if (!StringUtils.hasText(world.getCurrentTime())) {
                world.setCurrentTime(first.getTimeLabel());
            }
            if (!StringUtils.hasText(world.getCurrentPlace())) {
                world.setCurrentPlace(first.getPlace());
            }
            if (!StringUtils.hasText(world.getSummary())) {
                world.setSummary("穿书开始。当前接近原著节点：" + first.getOriginalPlot());
            }
        }
        worldStateRepository.save(world);
    }

    private void saveNodes(Long storyBaseId, List<ParsedNode> nodes) {
        int i = 1;
        for (ParsedNode n : nodes) {
            CanonNodeEntity entity = new CanonNodeEntity();
            entity.setStoryBaseId(storyBaseId);
            entity.setSeqNo(n.getSeqNo() == null ? i : n.getSeqNo());
            entity.setTimeLabel(n.getTimeLabel());
            entity.setPlace(n.getPlace());
            entity.setOriginalPlot(n.getOriginalPlot());
            entity.setStatus("PENDING");
            canonNodeRepository.save(entity);
            i++;
        }
    }

    private void replaceNodes(Long storyBaseId, List<UpsertStoryBaseRequest.CanonNodeInput> inputs) {
        canonNodeRepository.deleteByStoryBaseId(storyBaseId);
        int i = 1;
        for (UpsertStoryBaseRequest.CanonNodeInput input : inputs) {
            CanonNodeEntity entity = new CanonNodeEntity();
            entity.setStoryBaseId(storyBaseId);
            entity.setSeqNo(input.getSeqNo() == null ? i : input.getSeqNo());
            entity.setTimeLabel(input.getTimeLabel());
            entity.setPlace(input.getPlace());
            entity.setOriginalPlot(input.getOriginalPlot());
            entity.setStatus(StringUtils.hasText(input.getStatus()) ? input.getStatus() : "PENDING");
            canonNodeRepository.save(entity);
            i++;
        }
    }

    private StoryBaseResponse toResponse(StoryBaseEntity base) {
        List<CanonNodeEntity> nodes = canonNodeRepository.findByStoryBaseIdOrderBySeqNoAsc(base.getId());
        WorldStateEntity world = worldStateRepository.findByStoryBaseId(base.getId()).orElse(null);
        return StoryBaseResponse.from(base, nodes, world);
    }
}
