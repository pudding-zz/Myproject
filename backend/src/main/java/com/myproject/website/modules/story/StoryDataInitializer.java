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
        base.setTitle("示例：雨巷书店");
        base.setAuthor("原创示例");
        base.setBackground("现代都市。一座只在雨夜热闹的独立书店，藏着未说出口的故事。");
        base.setStatus("CONFIRMED");
        storyBaseRepository.save(base);

        saveNode(base.getId(), 1, "初遇夜", "书店门口", "陌生人躲雨推门而入，店主递上一杯热茶。");
        saveNode(base.getId(), 2, "三日后", "二楼诗架", "两人共读同一页诗，手指几乎碰到。");
        saveNode(base.getId(), 3, "暴雨夜", "书店打烊后", "原著中店主本应独自关门离开；客人未再出现。");
        saveNode(base.getId(), 4, "一周后", "城市另一端", "一次偶然重逢，决定是否继续往来。");
        saveNode(base.getId(), 5, "结局附近", "书店灯下", "原著走向：各自回到日常，留下未寄出的书签。");

        WorldStateEntity world = new WorldStateEntity();
        world.setStoryBaseId(base.getId());
        world.setCurrentTime("初遇夜");
        world.setCurrentPlace("书店门口");
        world.setPresentCharacters("店主、来客");
        world.setSummary("穿书开始。雨刚下起来，故事尚未偏离。");
        worldStateRepository.save(world);

        CharacterEntity host = new CharacterEntity();
        host.setStoryBaseId(base.getId());
        host.setName("顾晚星");
        host.setGender("female");
        host.setTitle("书店店主");
        host.setSetting("雨夜你推门进来时，她正在整理旧诗集。");
        host.setPersonality("温柔、敏锐，话少但句句有重量。");
        host.setPlayerInsert(false);
        host.setSystemPrompt("你是顾晚星，独立书店店主。");
        host.setEnabled(true);
        characterRepository.save(host);

        CharacterEntity self = new CharacterEntity();
        self.setStoryBaseId(base.getId());
        self.setName("我");
        self.setGender("other");
        self.setTitle("避雨的来客");
        self.setSetting("你因一场骤雨推开这家书店的门。");
        self.setPersonality("好奇、克制，愿意把故事写偏一点。");
        self.setPlayerInsert(true);
        self.setSystemPrompt("你是玩家代入角色「我」。");
        self.setEnabled(true);
        characterRepository.save(self);

        log.info("Seeded sample 剧情底本 and characters");
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
}
