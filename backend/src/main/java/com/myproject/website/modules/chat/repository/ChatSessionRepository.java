package com.myproject.website.modules.chat.repository;

import com.myproject.website.modules.chat.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {
}
