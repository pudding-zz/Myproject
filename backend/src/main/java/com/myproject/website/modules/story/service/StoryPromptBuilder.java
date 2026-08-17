package com.myproject.website.modules.story.service;

import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.story.entity.CanonNodeEntity;
import com.myproject.website.modules.story.entity.DivergenceLogEntity;
import com.myproject.website.modules.story.entity.StoryBaseEntity;
import com.myproject.website.modules.story.entity.WorldStateEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 穿书提示词（模式 A：玩家=所选角色，AI=世界与 NPC）。
 */
@Component
public class StoryPromptBuilder {

    public String buildContext(
            StoryBaseEntity base,
            List<CanonNodeEntity> nodes,
            WorldStateEntity world) {
        return buildContext(base, nodes, world, List.of());
    }

    public String buildContext(
            StoryBaseEntity base,
            List<CanonNodeEntity> nodes,
            WorldStateEntity world,
            List<DivergenceLogEntity> recentDivergences) {
        StringBuilder sb = new StringBuilder();
        sb.append("【剧情底本】《").append(base.getTitle()).append("》\n");
        if (StringUtils.hasText(base.getAuthor())) {
            sb.append("作者：").append(base.getAuthor()).append('\n');
        }
        sb.append("声明：").append(base.getDisclaimer()).append('\n');
        if (StringUtils.hasText(base.getBackground())) {
            sb.append("背景：").append(base.getBackground()).append('\n');
        }
        sb.append("\n【原著节点】\n");
        for (CanonNodeEntity n : nodes) {
            sb.append(n.getSeqNo()).append(". ")
                    .append(nullToEmpty(n.getTimeLabel())).append(" / ")
                    .append(nullToEmpty(n.getPlace())).append(" | 状态=")
                    .append(n.getStatus()).append('\n')
                    .append("  原著走向：").append(n.getOriginalPlot()).append('\n');
            if (StringUtils.hasText(n.getChangedPlot())) {
                sb.append("  已改写为：").append(n.getChangedPlot()).append('\n');
            }
        }
        sb.append("\n【当前世界】\n");
        if (world == null) {
            sb.append("尚未初始化。\n");
        } else {
            sb.append("时间：").append(nullToEmpty(world.getCurrentTime())).append('\n');
            sb.append("地点：").append(nullToEmpty(world.getCurrentPlace())).append('\n');
            sb.append("在场：").append(nullToEmpty(world.getPresentCharacters())).append('\n');
            sb.append("摘要：").append(nullToEmpty(world.getSummary())).append('\n');
        }
        if (recentDivergences != null && !recentDivergences.isEmpty()) {
            sb.append("\n【近期偏离】\n");
            for (DivergenceLogEntity d : recentDivergences) {
                sb.append("- ").append(nullToEmpty(d.getNewText())).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 模式 A：用户消息即所选角色言行；模型只演世界与其他 NPC。
     */
    public String buildPlayerPerspectiveSystemPrompt(CharacterEntity character, String storyContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「穿书」世界的叙事与 NPC 引擎。\n");
        sb.append("玩家已选定角色「").append(character.getName()).append("」，");
        sb.append("用户每条消息都是该角色的台词或行动（第一人称即该角色本人）。\n");
        sb.append("你必须：输出旁白、场景反应、其他人物台词与行动；");
        sb.append("禁止以「").append(character.getName()).append("」的口吻说话或替玩家做决定。\n");
        if (StringUtils.hasText(character.getGender())) {
            sb.append("玩家角色性别：").append(character.getGender()).append('\n');
        }
        if (StringUtils.hasText(character.getTitle())) {
            sb.append("玩家角色身份：").append(character.getTitle()).append('\n');
        }
        if (StringUtils.hasText(character.getPersonality())) {
            sb.append("玩家角色人设（供你把握其言行是否合理）：")
                    .append(character.getPersonality()).append('\n');
        }
        if (StringUtils.hasText(character.getSetting())) {
            sb.append("玩家角色设定：").append(character.getSetting()).append('\n');
        }
        if (Boolean.TRUE.equals(character.getPlayerInsert())) {
            sb.append("这是玩家原创插入角色，可逐步改写原著重心。\n");
        }
        sb.append('\n').append(storyContext).append('\n');
        sb.append("""
                规则：
                1. 你知道当前时段原著本该发生什么，也知道【当前世界】与【近期偏离】；
                2. 玩家行动可能导致大事件被改写、取消或替换，要自然体现穿书变化；
                3. 若玩家行为已使某原著节点不再发生，后续勿假装该事件仍发生；
                4. 不要声称官方正版；不要大段复述原著原文；
                5. 回复简洁有戏，旁白与 NPC 对白分开写清。
                """);
        return sb.toString();
    }

    public String buildAdvanceUserPrompt(String characterName, String playerAction) {
        return """
                玩家（作为「%s」）的动作/推进：%s

                请推进一段世界与 NPC 的反应（不要用「%s」的口吻说话），并在末尾单独追加两行（不要放入对白里）：
                DIVERGENCE: 原著本应…… → 现在变成……
                若本次几乎无偏离，写：DIVERGENCE: 无
                若原著事件被跳过/未发生，写清楚「跳过」或「未发生」，例如：DIVERGENCE: 原著本应退婚 → 现在未发生/跳过
                再追加一行：
                WORLD: 时间=...;地点=...;在场=...;摘要=...
                """.formatted(characterName, playerAction, characterName);
    }

    public String buildChatUserPrompt(String characterName, String playerLine) {
        return """
                玩家（作为「%s」）说/做：%s

                请只输出世界旁白与其他 NPC 的反应，不要以「%s」的口吻回复。
                """.formatted(characterName, playerLine, characterName);
    }

    public String buildTheaterSpeakerPrompt(CharacterEntity speaker, String storyContext, String transcript) {
        return """
                你是导演安排的演员，只扮演「%s」。
                人设：%s
                身份：%s

                %s

                已有对白：
                %s

                规则：只输出该角色下一句/小段台词或动作，推动剧情；不要替别人说话；保持与【当前世界】一致。
                """.formatted(
                speaker.getName(),
                nullToEmpty(speaker.getPersonality()),
                nullToEmpty(speaker.getTitle()),
                storyContext,
                StringUtils.hasText(transcript) ? transcript : "（场景刚开始）");
    }

    public String buildTheaterDirectorSettlePrompt(String storyContext, String transcript) {
        return """
                你是穿书导演。根据以下对戏台词，结算本场对世界与原著的影响。

                %s

                【本场对白】
                %s

                只输出两行，不要其它内容：
                DIVERGENCE: 原著本应…… → 现在变成……
                若几乎无偏离：DIVERGENCE: 无
                若事件被跳过：写明跳过/未发生
                WORLD: 时间=...;地点=...;在场=...;摘要=...
                """.formatted(storyContext, StringUtils.hasText(transcript) ? transcript : "（无对白）");
    }

    public String summarizeNodes(List<CanonNodeEntity> nodes) {
        return nodes.stream()
                .map(n -> n.getSeqNo() + "." + n.getOriginalPlot())
                .collect(Collectors.joining(" | "));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
