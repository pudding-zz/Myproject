package com.myproject.website.modules.story.repository;

import com.myproject.website.modules.story.entity.DivergenceLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DivergenceLogRepository extends JpaRepository<DivergenceLogEntity, Long> {

    List<DivergenceLogEntity> findByStoryBaseIdOrderByIdDesc(Long storyBaseId);
}
