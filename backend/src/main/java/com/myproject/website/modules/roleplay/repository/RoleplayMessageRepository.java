package com.myproject.website.modules.roleplay.repository;

import com.myproject.website.modules.roleplay.entity.RoleplayMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleplayMessageRepository extends JpaRepository<RoleplayMessageEntity, Long> {

    List<RoleplayMessageEntity> findBySessionIdOrderByIdAsc(Long sessionId);
}
