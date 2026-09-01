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

    /** 状态结算器：只输出 JSON，不扮演角色。 */
    public String buildStatusSettleSystemPrompt() {
        return """
                你是「角色状态结算器」，不是对话角色。
                根据最近对话与当前状态快照，输出一个 JSON 对象，用于更新角色状态面板。
                硬性规则：
                1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要解释文字；
                2. 未在对话中出现的内容不要编造；没有变化时 changed=false，status 可省略或为空对象；
                3. status 里只填写需要更新的键（可缺省）：blocks, intimacy, life, favorability, favorOs, forum, theater, misc, access；
                4. life 项结构：{"title":"进食|睡眠|礼物|约定","lines":["..."],"os":"..."}；若更新 life，请给出完整四块或至少给出有变化的块（后端会按 title 合并）；
                5. healthPatch 可选：{"day":1-31,"cal":0,"heart":0,"count":0,"duration":0,"trigger":"","scene":"","thought":""}，仅当对话明确涉及亲密/生理事件时填写；
                6. note 用一句中文说明本次整理了什么（给用户看）。
                JSON 形状示例：
                {"changed":true,"note":"根据对话更新了约定","status":{"life":[{"title":"约定","lines":["周末去旧书店"],"os":"他主动提的"}]},"healthPatch":null}
                """;
    }

    public String buildStatusSettleUserPrompt(
            RoleplaySessionEntity session, String currentStatusJson, String transcript) {
        return """
                AI角色：%s
                玩家角色：%s
                场景：%s

                【当前状态 JSON】
                %s

                【最近对话】
                %s

                请输出结算 JSON。
                """.formatted(
                session.getAiName(),
                session.getPlayerName(),
                StringUtils.hasText(session.getScene()) ? session.getScene() : "（未设定）",
                currentStatusJson,
                StringUtils.hasText(transcript) ? transcript : "（暂无对话）");
    }
}
