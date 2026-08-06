package com.myproject.website.modules.story.service;

import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.story.entity.CanonNodeEntity;
import com.myproject.website.modules.story.entity.StoryBaseEntity;
import com.myproject.website.modules.story.entity.WorldStateEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StoryPromptBuilder {

    public String buildContext(
            StoryBaseEntity base,
            List<CanonNodeEntity> nodes,
            WorldStateEntity world) {
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
        return sb.toString();
    }

    public String buildCharacterSystemPrompt(CharacterEntity character, String storyContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你正在参与「穿书」互动。请严格以角色「")
                .append(character.getName())
                .append("」的身份说话与行动。\n");
        if (StringUtils.hasText(character.getGender())) {
            sb.append("性别：").append(character.getGender()).append('\n');
        }
        if (StringUtils.hasText(character.getTitle())) {
            sb.append("身份：").append(character.getTitle()).append('\n');
        }
        if (StringUtils.hasText(character.getPersonality())) {
            sb.append("人设：").append(character.getPersonality()).append('\n');
        }
        if (StringUtils.hasText(character.getSetting())) {
            sb.append("相遇/设定：").append(character.getSetting()).append('\n');
        }
        if (Boolean.TRUE.equals(character.getPlayerInsert())) {
            sb.append("这是玩家代入角色；回应时把玩家当本人互动。\n");
        }
        sb.append('\n').append(storyContext).append('\n');
        sb.append("""
                规则：
                1. 你知道当前时间段原著本该发生什么，也知道【当前世界】里已经发生的偏离；
                2. 玩家推进可能导致大事件被改写、取消或替换，你要自然体现「穿书」变化；
                3. 不要声称这是官方正版；不要大段复述原著原文；
                4. 回复简洁有戏，可带少量动作描写。
                """);
        return sb.toString();
    }

    public String buildAdvanceUserPrompt(String playerAction) {
        return """
                玩家动作/推进：%s

                请以角色身份推进一段互动，并在末尾单独追加一行（不要放入对白里）：
                DIVERGENCE: 原著本应…… → 现在变成……
                若本次几乎无偏离，写：DIVERGENCE: 无
                再追加一行：
                WORLD: 时间=...;地点=...;摘要=...
                """.formatted(playerAction);
    }

    public String buildTheaterSpeakerPrompt(CharacterEntity speaker, String storyContext, String transcript) {
        return """
                你是导演安排的演员，只扮演「%s」。
                %s
                人设：%s
                身份：%s

                %s

                已有对白：
                %s

                规则：只输出该角色下一句/小段台词或动作，推动剧情；不要替别人说话；保持与【当前世界】一致。
                """.formatted(
                speaker.getName(),
                Boolean.TRUE.equals(speaker.getPlayerInsert()) ? "（玩家代入角，可被其他角色互动）" : "",
                nullToEmpty(speaker.getPersonality()),
                nullToEmpty(speaker.getTitle()),
                storyContext,
                StringUtils.hasText(transcript) ? transcript : "（场景刚开始）");
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
