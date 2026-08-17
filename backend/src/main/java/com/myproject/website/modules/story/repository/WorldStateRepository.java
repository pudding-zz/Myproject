package com.myproject.website.modules.story.repository;

import com.myproject.website.modules.story.entity.WorldStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorldStateRepository extends JpaRepository<WorldStateEntity, Long> {

    Optional<WorldStateEntity> findByStoryBaseId(Long storyBaseId);
}
