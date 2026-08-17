package com.myproject.website.modules.story.repository;

import com.myproject.website.modules.story.entity.StoryBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryBaseRepository extends JpaRepository<StoryBaseEntity, Long> {

    List<StoryBaseEntity> findAllByOrderByIdDesc();
}
