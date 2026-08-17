package com.myproject.website.modules.roleplay.repository;

import com.myproject.website.modules.roleplay.entity.RoleplayHealthRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleplayHealthRecordRepository extends JpaRepository<RoleplayHealthRecordEntity, Long> {

    List<RoleplayHealthRecordEntity> findBySessionIdOrderByDayAsc(Long sessionId);

    Optional<RoleplayHealthRecordEntity> findBySessionIdAndDay(Long sessionId, Integer day);

    void deleteBySessionId(Long sessionId);
}
