package com.myproject.website.modules.story.repository;

import com.myproject.website.modules.story.entity.CanonNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanonNodeRepository extends JpaRepository<CanonNodeEntity, Long> {

    List<CanonNodeEntity> findByStoryBaseIdOrderBySeqNoAsc(Long storyBaseId);

    void deleteByStoryBaseId(Long storyBaseId);
}
