package com.myproject.website.modules.ai;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 演示模式 Mock 回复：当 DeepSeek API Key 未配置时启用。
 * 根据 system prompt 的特征判断当前场景，返回合理的预设内容，
 * 让用户能完整体验穿书对话/推进/偏离/对戏/书名取纲全链路。
 */
public class MockAiClient implements AiClient {

    @Override
    public String chat(List<AiMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String system = messages.stream()
                .filter(m -> "system".equals(m.getRole()))
                .map(AiMessage::getContent)
                .reduce("", (a, b) -> a + "\n" + b);
        String lastUser = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .reduce((first, second) -> second)
                .map(AiMessage::getContent)
                .orElse("");

        // 场景 0：角色状态结算 —— 只输出 JSON
        if (system.contains("角色状态结算器") || lastUser.contains("请输出结算 JSON")) {
            return mockStatusSettle();
        }

        // 场景 1：书名取纲 —— 要求输出 JSON
        if (system.contains("剧情整理助手") || lastUser.contains("整理一份「剧情底本」粗纲")) {
            return mockOutline(lastUser);
        }

        // 场景 2：AI 对戏 —— 导演结算（DIVERGENCE + WORLD 两行）
        if (system.contains("穿书导演") || (lastUser.contains("本场对白") && lastUser.contains("结算本场"))) {
            return mockTheaterSettle();
        }

        // 场景 3：角色扮演双人对话
        if (system.contains("双人角色扮演对话")) {
            return mockRoleplayLine(system);
        }

        // 场景 4：AI 对戏 —— 单个演员下一句台词
        if (system.contains("导演安排的演员") || system.contains("只扮演「")) {
            return mockTheaterSpeakerLine(system);
        }

        // 场景 5：穿书模式 A（玩家视角叙事引擎）
        boolean advance = lastUser.contains("DIVERGENCE:") || lastUser.contains("WORLD:");
        return mockPlayerPerspective(system, lastUser, advance);
    }

    private static String mockStatusSettle() {
        return """
                {
                  "changed": true,
                  "note": "根据最近对话整理了进食与约定（演示模式）",
                  "status": {
                    "life": [
                      {"title": "进食", "lines": ["上次：便利店关东煮 + 茶叶蛋", "饱腹值：58%"], "os": "嘴上说随便，手却点了粥。"},
                      {"title": "睡眠", "lines": ["上床偏晚", "困倦值：65%"], "os": "夜班后还能聊这么久。"},
                      {"title": "礼物", "lines": ["暂无新增"], "os": ""},
                      {"title": "约定", "lines": ["下次值班结束一起走"], "os": "是他先提出的。"}
                    ]
                  },
                  "healthPatch": null
                }
                """;
    }

    private static String mockRoleplayLine(String system) {
        String name = "对方";
        int idx = system.indexOf("只扮演「");
        if (idx >= 0) {
            int start = idx + "只扮演「".length();
            int end = system.indexOf('」', start);
            if (end > start) {
                name = system.substring(start, end);
            }
        }
        String[] lines = {
                name + "抬眼看了你一下：「……说话。」",
                name + "把视线移开，声音很低：「我在听。」",
                "「别站门口吹风。」" + name + "把白大褂衣领拢了拢。",
                name + "沉默两秒：「你想说什么，直接说。」"
        };
        return lines[ThreadLocalRandom.current().nextInt(lines.length)];
    }

    // ──────────────────────────────── 场景实现 ────────────────────────────────

    private static String mockOutline(String userPrompt) {
        String title = extractTitle(userPrompt);
        String author = extractAuthor(userPrompt);
        boolean isDoupo = title.contains("斗破") || title.isEmpty();
        String background;
        String nodesJson;
        if (isDoupo) {
            title = StringUtils.hasText(title) ? title : "斗破苍穹";
            background = "斗气大陆，以修炼斗气为尊。乌坦城萧家少年萧炎曾是百年难遇的天才，却在十五岁修为莫名跌落，遭未婚妻云岚宗纳兰嫣然当众退婚羞辱。其后得寄居戒指中的药老相助，重踏修炼之路，闯荡大陆，最终成就斗帝之名。";
            nodesJson = """
                    [
                      {"seqNo":1,"timeLabel":"乌坦城·退婚当日","place":"萧家大殿","originalPlot":"纳兰嫣然当众退婚，萧炎受辱立下三年之约。"},
                      {"seqNo":2,"timeLabel":"退婚之后","place":"萧家后院/密室","originalPlot":"药老现身，萧炎获得重新变强的契机，开始隐秘修炼并学习炼药术。"},
                      {"seqNo":3,"timeLabel":"家族试炼前后","place":"乌坦城周边","originalPlot":"萧炎实力回升，与加列家族、米特尔拍卖场等势力交锋，云岚宗压力仍在。"},
                      {"seqNo":4,"timeLabel":"离开乌坦城","place":"乌坦城城门","originalPlot":"萧炎告别萧薰儿与家族，踏上更广阔的斗气大陆寻找异火与机缘。"},
                      {"seqNo":5,"timeLabel":"闯荡初期","place":"魔兽山脉一带","originalPlot":"历练与奇遇，结识小医仙、获取紫晶翼狮王内丹等，实力与名声开始积累。"},
                      {"seqNo":6,"timeLabel":"迦南学院阶段","place":"迦南学院/内院","originalPlot":"入学院修炼、结识磐门同伴，获得陨落心炎，卷入学院内外势力纠葛。"},
                      {"seqNo":7,"timeLabel":"云岚线转折","place":"云岚宗/加玛帝国","originalPlot":"三年之约赴约，与云韵/云山恩怨再起，退婚旧约引发更大对峙，覆灭云岚。"},
                      {"seqNo":8,"timeLabel":"大陆征途","place":"中州方向","originalPlot":"走向更远端的大陆舞台，争丹塔、闯古界、对抗魂殿，原著主线继续远征。"}
                    ]""";
        } else {
            background = "（Mock 演示底本）这是一本名为《" + title + "》的小说"
                    + (StringUtils.hasText(author) ? "，作者" + author : "")
                    + "。故事围绕主角展开，世界观、力量体系、主要冲突如下方节点所述，仅供私人娱乐参考。";
            nodesJson = """
                    [
                      {"seqNo":1,"timeLabel":"开局","place":"起点场景","originalPlot":"主角登场，交代身份与困境，核心冲突初现端倪。"},
                      {"seqNo":2,"timeLabel":"机遇","place":"秘境/长者指引","originalPlot":"主角获得关键机缘或遇到引路人，实力/认知发生跃迁。"},
                      {"seqNo":3,"timeLabel":"第一次考验","place":"竞技场/秘境/战场","originalPlot":"主角面临第一次真正的考验，在失败边缘突破自我并取胜。"},
                      {"seqNo":4,"timeLabel":"主线卷入","place":"更广的舞台","originalPlot":"事件升级，主角被迫/主动卷入更大的势力纷争，走出新手区。"},
                      {"seqNo":5,"timeLabel":"友情与背叛","place":"多方交汇之地","originalPlot":"同伴聚散、敌友关系发生转折，主角价值观受到挑战。"},
                      {"seqNo":6,"timeLabel":"大高潮前夕","place":"决战地","originalPlot":"多方势力齐聚，最终大战的前夕，各自的底牌与牺牲浮出水面。"}
                    ]""";
        }
        return """
                {
                  "title": "%s",
                  "author": "%s",
                  "background": "%s",
                  "nodes": %s
                }
                """.formatted(
                escapeJson(title),
                escapeJson(author),
                escapeJson(background),
                nodesJson);
    }

    private static String mockTheaterSpeakerLine(String system) {
        String name = "某人";
        int start = system.indexOf("只扮演「");
        if (start >= 0) {
            int end = system.indexOf("」", start + 5);
            if (end > start + 5) name = system.substring(start + 5, end);
        }
        String[] lines = {
                "微微皱眉，抬眼看向对方：「事情恐怕没有你说的那么简单，但既然你已开口，我便陪你走这一趟。」",
                "冷笑一声，负手而立：「三年前的账，今日也该算一算了。不要以为背后有靠山，就能在这里撒野。」",
                "指尖轻轻敲着桌面，若有所思：「此事背后另有其人，我们不妨先按兵不动，看看到底是谁在渔翁得利。」",
                "目光一沉，语气带着几分警告：「劝你识相些，在这斗气大陆上，可不是什么人都得罪得起的。」",
                "叹了口气，神色复杂：「我并非不愿帮你，只是此事牵连太广，稍有不慎便是万劫不复。」",
                "神色微动，忽而轻笑：「有意思，既然你敢赌，那我也不妨下注。成则举杯相庆，败则……我不会让你一个人担着。」",
        };
        String line = lines[ThreadLocalRandom.current().nextInt(lines.length)];
        // 偶尔带点动作描写
        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            return "（袖中拳头缓缓收紧）" + line;
        }
        return line;
    }

    private static String mockTheaterSettle() {
        String[] divergences = {
                "原著本应主角忍气离去 → 现在改为当场立下新约，三月后再决胜负，节点被改写。",
                "原著本应退婚风波迅速平息 → 现在第三方势力突然介入，矛盾升级，节点被改写。",
                "无",
                "原著本应剧情推进至离开乌坦城 → 现在因新增角色插话，原定行程推迟三日，节点略改。"
        };
        String d = divergences[ThreadLocalRandom.current().nextInt(divergences.length)];
        return "DIVERGENCE: " + d + "\n"
                + "WORLD: 时间=对峙升级时刻;地点=萧家大殿/偏厅;在场=萧炎、纳兰嫣然、萧家众人、插入角色;摘要=对戏后氛围凝重，原定退婚仪式未完全按原著走向结束，各方关系出现新的张力。";
    }

    private static String mockPlayerPerspective(String system, String lastUser, boolean advance) {
        // 提取玩家角色名
        String playerName = "你所扮演的角色";
        int idx = system.indexOf("玩家已选定角色「");
        if (idx < 0) idx = system.indexOf("玩家（作为「");
        if (idx >= 0) {
            int close = system.indexOf("」", idx + 8);
            if (close > idx) {
                playerName = system.substring(idx + 8, close);
            }
        }

        // 根据用户输入长度决定回复丰富度
        boolean isAction = lastUser.contains("（推进剧情）")
                || lastUser.contains("推进剧情")
                || lastUser.contains("退婚")
                || lastUser.contains("纳兰")
                || lastUser.contains("三年之约");

        StringBuilder sb = new StringBuilder();
        sb.append("【旁白】萧家大殿之上，众人目光纷纷落在").append(playerName).append("身上。\n");
        if (isAction) {
            sb.append("纳兰嫣然秀眉微蹙，似乎没有料到对方会是这样的回应。她身后的云岚宗老者低声道：「嫣然，莫要与此人多费唇舌。」\n");
            sb.append("\n").append("纳兰嫣然：「").append(playerName).append("，我知你心有不甘，但这桩婚事本就非你我所愿。今日我亲自登门，已是给足萧家颜面。」\n");
            sb.append("\n").append("【旁白】萧战面色铁青，却碍于云岚宗之势不敢发作；几位长老神色各异，有的面露愤慨，有的暗存观望。\n");
            sb.append("人群之外，一枚古朴戒指在").append(playerName).append("袖中微微发烫——那是药老即将苏醒的征兆。\n");
        } else {
            sb.append("周遭气息一时微凝，似乎被").append(playerName).append("方才的言行所惊。\n");
            sb.append("\n").append("萧战（低声）：「").append(playerName).append("，切莫冲动。」\n");
            sb.append("\n").append("【旁白】然而局势已如弦上之箭，再难收回。");
        }

        if (advance) {
            // 推进剧情：末尾追加 DIVERGENCE + WORLD 两行
            String[] options = {
                    "DIVERGENCE: 原著本应退婚仪式如常结束 → 现在因玩家举动，纳兰嫣然决定再留半日观察，节点改写。\n"
                            + "WORLD: 时间=退婚当日午后;地点=萧家大殿;在场=萧炎、纳兰嫣然、萧家众人、云岚宗随行;摘要=退婚流程被打断，玩家行为引起纳兰嫣然额外留意，药老的残魂在戒指中开始苏醒。",
                    "DIVERGENCE: 无\n"
                            + "WORLD: 时间=退婚当日;地点=萧家大殿;在场=萧炎、纳兰嫣然、萧家众人;摘要=剧情基本贴近原著，三年之约已在空气中成形，只待一句话落定。",
                    "DIVERGENCE: 原著本应退婚 → 现在因玩家强硬态度，婚未退成，改日再议，节点被改写。\n"
                            + "WORLD: 时间=退婚当日;地点=萧家后堂;在场=萧炎、纳兰嫣然、萧战、云岚宗长老;摘要=婚未退成，双方关系更趋紧张，云岚宗方面开始重新评估萧家与玩家角色。"
            };
            String tail = options[ThreadLocalRandom.current().nextInt(options.length)];
            sb.append("\n\n").append(tail);
        }
        return sb.toString();
    }

    // ──────────────────────────────── 工具 ────────────────────────────────

    private static String extractTitle(String prompt) {
        int i = prompt.indexOf("《");
        if (i < 0) return "";
        int j = prompt.indexOf("》", i + 1);
        if (j < 0) return "";
        return prompt.substring(i + 1, j).trim();
    }

    private static String extractAuthor(String prompt) {
        int i = prompt.indexOf("作者：");
        if (i < 0) i = prompt.indexOf("（作者：");
        if (i < 0) return "";
        int start = i + (prompt.charAt(i) == '（' ? 4 : 3);
        int j = prompt.indexOf("）", start);
        String s = j < 0 ? prompt.substring(start) : prompt.substring(start, j);
        return s.trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
