package com.myproject.website.modules.chat.repository;

import com.myproject.website.modules.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByChatIdOrderByIdAsc(Long chatId);
}
