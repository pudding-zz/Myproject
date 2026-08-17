package com.myproject.website.modules.roleplay.repository;

import com.myproject.website.modules.roleplay.entity.RoleplaySessionStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleplaySessionStatusRepository extends JpaRepository<RoleplaySessionStatusEntity, Long> {

    Optional<RoleplaySessionStatusEntity> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
