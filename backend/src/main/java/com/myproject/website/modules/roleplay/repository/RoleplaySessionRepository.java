package com.myproject.website.modules.roleplay.repository;

import com.myproject.website.modules.roleplay.entity.RoleplaySessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleplaySessionRepository extends JpaRepository<RoleplaySessionEntity, Long> {

    List<RoleplaySessionEntity> findAllByOrderByUpdatedAtDesc();
}
