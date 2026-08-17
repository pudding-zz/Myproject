package com.myproject.website.modules.character.service;

import com.myproject.website.common.BusinessException;
import com.myproject.website.common.ErrorCode;
import com.myproject.website.modules.character.dto.CharacterResponse;
import com.myproject.website.modules.character.dto.CreateCharacterRequest;
import com.myproject.website.modules.character.dto.UpdateCharacterRequest;
import com.myproject.website.modules.character.entity.CharacterEntity;
import com.myproject.website.modules.character.repository.CharacterRepository;
import com.myproject.website.modules.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final StoryService storyService;

    @Transactional(readOnly = true)
    public List<CharacterResponse> listEnabled(Long storyBaseId) {
        List<CharacterEntity> list = storyBaseId == null
                ? characterRepository.findByEnabledTrueOrderByIdAsc()
                : characterRepository.findByStoryBaseIdAndEnabledTrueOrderByIdAsc(storyBaseId);
        return list.stream().map(CharacterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse getById(Long id) {
        return CharacterResponse.from(requireEnabled(id));
    }

    @Transactional
    public CharacterResponse create(CreateCharacterRequest request) {
        storyService.requireConfirmed(request.getStoryBaseId());

        CharacterEntity entity = new CharacterEntity();
        entity.setStoryBaseId(request.getStoryBaseId());
        entity.setName(request.getName().trim());
        entity.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : "male");
        entity.setTitle(request.getTitle());
        entity.setSetting(request.getSetting());
        entity.setPersonality(request.getPersonality());
        entity.setPlayerInsert(Boolean.TRUE.equals(request.getPlayerInsert()));
        entity.setEnabled(true);
        entity.setSystemPrompt(buildDefaultPrompt(entity));
        characterRepository.save(entity);
        return CharacterResponse.from(entity);
    }

    @Transactional
    public CharacterResponse update(Long id, UpdateCharacterRequest request) {
        CharacterEntity entity = requireEnabled(id);
        entity.setName(request.getName().trim());
        entity.setGender(StringUtils.hasText(request.getGender()) ? request.getGender() : entity.getGender());
        entity.setTitle(request.getTitle());
        entity.setSetting(request.getSetting());
        entity.setPersonality(request.getPersonality());
        if (request.getPlayerInsert() != null) {
            entity.setPlayerInsert(request.getPlayerInsert());
        }
        entity.setSystemPrompt(buildDefaultPrompt(entity));
        characterRepository.save(entity);
        return CharacterResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public CharacterEntity requireEnabled(Long id) {
        CharacterEntity entity = characterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "character not found"));
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "character not found");
        }
        return entity;
    }

    private String buildDefaultPrompt(CharacterEntity entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是角色「").append(entity.getName()).append("」。");
        if (StringUtils.hasText(entity.getTitle())) {
            sb.append("身份：").append(entity.getTitle()).append('。');
        }
        if (StringUtils.hasText(entity.getPersonality())) {
            sb.append("人设：").append(entity.getPersonality());
        }
        return sb.toString();
    }
}
