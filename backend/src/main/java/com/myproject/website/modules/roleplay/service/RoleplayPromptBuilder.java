package com.myproject.website.modules.roleplay.service;

import com.myproject.website.modules.roleplay.entity.RoleplaySessionEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleplayPromptBuilder {

    public String buildSystemPrompt(RoleplaySessionEntity session) {
        StringBuilder sb = new StringBuilder();
        sb.append("你正在进行双人角色扮演对话。\n");
        sb.append("你只扮演「").append(session.getAiName()).append("」，不要替玩家说话。\n");
        if (StringUtils.hasText(session.getAiGender())) {
            sb.append("性别：").append(session.getAiGender()).append('\n');
        }
        if (StringUtils.hasText(session.getAiTitle())) {
            sb.append("身份：").append(session.getAiTitle()).append('\n');
        }
        if (StringUtils.hasText(session.getAiPersonality())) {
            sb.append("性格：").append(session.getAiPersonality()).append('\n');
        }
        if (StringUtils.hasText(session.getAiRelation())) {
            sb.append("与玩家的关系：").append(session.getAiRelation()).append('\n');
        }
        sb.append('\n');
        sb.append("玩家扮演「").append(session.getPlayerName()).append("」。\n");
        if (StringUtils.hasText(session.getPlayerGender())) {
            sb.append("玩家性别：").append(session.getPlayerGender()).append('\n');
        }
        if (StringUtils.hasText(session.getPlayerTitle())) {
            sb.append("玩家身份：").append(session.getPlayerTitle()).append('\n');
        }
        if (StringUtils.hasText(session.getPlayerPersonality())) {
            sb.append("玩家性格：").append(session.getPlayerPersonality()).append('\n');
        }
        if (StringUtils.hasText(session.getPlayerRelation())) {
            sb.append("玩家侧关系说明：").append(session.getPlayerRelation()).append('\n');
        }
        if (StringUtils.hasText(session.getScene())) {
            sb.append('\n').append("当前场景：").append(session.getScene()).append('\n');
        }
        sb.append("""

                规则：
                1. 始终保持「%s」的口吻、性格与关系张力；
                2. 回复简洁有戏，可含简短动作描写，但不要写成长篇小说；
                3. 不要输出系统说明、不要替玩家做决定或代写玩家台词；
                4. 不要声称官方正版或现实真人身份。
                """.formatted(session.getAiName()));
        return sb.toString();
    }

    public String buildOpeningUserPrompt(RoleplaySessionEntity session) {
        return """
                请以「%s」的身份说一句自然的开场白，开启与「%s」的对话。
                只输出角色台词/动作，不要加旁白标题。
                """.formatted(session.getAiName(), session.getPlayerName());
    }

    public String buildChatUserPrompt(RoleplaySessionEntity session, String playerLine) {
        return """
                玩家（作为「%s」）说/做：%s

                请只以「%s」回应。
                """.formatted(session.getPlayerName(), playerLine, session.getAiName());
    }
}
