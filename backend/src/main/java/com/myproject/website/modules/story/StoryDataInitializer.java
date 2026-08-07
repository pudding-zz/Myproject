package com.myproject.website.modules.story;

import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.character.repository.CharacterRepository;
import com.myproject.website.modules.story.entity.CanonNodeEntity;
import com.myproject.website.modules.story.entity.StoryBaseEntity;
import com.myproject.website.modules.story.entity.WorldStateEntity;
import com.myproject.website.modules.story.repository.CanonNodeRepository;
import com.myproject.website.modules.story.repository.StoryBaseRepository;
import com.myproject.website.modules.story.repository.WorldStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoryDataInitializer implements ApplicationRunner {

    private final StoryBaseRepository storyBaseRepository;
    private final CanonNodeRepository canonNodeRepository;
    private final WorldStateRepository worldStateRepository;
    private final CharacterRepository characterRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (storyBaseRepository.count() > 0) {
            return;
        }

        StoryBaseEntity base = new StoryBaseEntity();
        base.setTitle("斗破苍穹");
        base.setAuthor("天蚕土豆");
        base.setBackground(
                "斗气大陆。乌坦城萧家少年萧炎曾是天才，后修为跌落，遭云岚宗纳兰嫣然退婚羞辱。"
                        + "其后得药老相助重踏修炼之路，闯荡大陆。本底本为非官方粗纲，仅私人娱乐。");
        base.setStatus("CONFIRMED");
        storyBaseRepository.save(base);

        saveNode(base.getId(), 1, "乌坦城·退婚当日", "萧家大殿",
                "纳兰嫣然当众退婚，萧炎受辱立下三年之约。");
        saveNode(base.getId(), 2, "退婚之后", "萧家后院/密室",
                "药老现身，萧炎获得重新变强的契机，开始隐秘修炼。");
        saveNode(base.getId(), 3, "家族试炼前后", "乌坦城周边",
                "萧炎实力回升，与家族内外冲突升温，云岚宗压力仍在。");
        saveNode(base.getId(), 4, "离开乌坦城", "乌坦城城门",
                "萧炎告别家族，踏上更广阔的斗气大陆。");
        saveNode(base.getId(), 5, "闯荡初期", "魔兽山脉一带",
                "历练与奇遇，实力与名声开始积累。");
        saveNode(base.getId(), 6, "迦南学院阶段", "迦南学院",
                "入学院修炼、结识同伴，卷入学院内外势力纠葛。");
        saveNode(base.getId(), 7, "云岚线转折", "云岚宗相关",
                "与云韵/云岚宗恩怨再起，退婚旧约引发更大对峙。");
        saveNode(base.getId(), 8, "大陆征途", "中州方向（概括）",
                "走向更远端的大陆舞台，原著主线继续远征。");

        WorldStateEntity world = new WorldStateEntity();
        world.setStoryBaseId(base.getId());
        world.setCurrentTime("乌坦城·退婚当日");
        world.setCurrentPlace("萧家大殿");
        world.setPresentCharacters("萧炎、纳兰嫣然、萧家众人");
        world.setSummary("穿书开始。退婚仪式即将或正在进行，故事尚未偏离。");
        worldStateRepository.save(world);

        saveCharacter(base.getId(), "萧炎", "male", "萧家少年",
                "曾是天才，如今修为跌落，正站在退婚风波中央。",
                "隐忍、好强，受辱后不服输。", false);
        saveCharacter(base.getId(), "纳兰嫣然", "female", "云岚宗弟子",
                "前来萧家退婚的女子，身后是云岚宗的压力。",
                "清傲、果决，不愿受家族联姻束缚。", false);
        saveCharacter(base.getId(), "药老", "male", "药尘残魂",
                "寄居戒指中的老前辈，尚未或即将与萧炎正式相遇。",
                "睿智、毒舌，看重潜力与心性。", false);

        log.info("Seeded 《斗破苍穹》剧情底本 and characters");
    }

    private void saveNode(Long baseId, int seq, String time, String place, String plot) {
        CanonNodeEntity node = new CanonNodeEntity();
        node.setStoryBaseId(baseId);
        node.setSeqNo(seq);
        node.setTimeLabel(time);
        node.setPlace(place);
        node.setOriginalPlot(plot);
        node.setStatus("PENDING");
        canonNodeRepository.save(node);
    }

    private void saveCharacter(
            Long baseId,
            String name,
            String gender,
            String title,
            String setting,
            String personality,
            boolean playerInsert) {
        CharacterEntity c = new CharacterEntity();
        c.setStoryBaseId(baseId);
        c.setName(name);
        c.setGender(gender);
        c.setTitle(title);
        c.setSetting(setting);
        c.setPersonality(personality);
        c.setPlayerInsert(playerInsert);
        c.setSystemPrompt("玩家可选角色「" + name + "」，选中后由玩家本人扮演。");
        c.setEnabled(true);
        characterRepository.save(c);
    }
}
