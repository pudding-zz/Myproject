package com.myproject.website.modules.character.repository;

import com.myproject.website.modules.character.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {

    List<CharacterEntity> findByEnabledTrueOrderByIdAsc();

    List<CharacterEntity> findByStoryBaseIdAndEnabledTrueOrderByIdAsc(Long storyBaseId);
}
