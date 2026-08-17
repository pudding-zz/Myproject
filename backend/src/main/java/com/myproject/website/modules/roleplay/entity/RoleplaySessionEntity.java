package com.myproject.website.modules.roleplay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "roleplay_sessions")
public class RoleplaySessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 64)
    private String userId = "local";

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "ai_name", nullable = false, length = 64)
    private String aiName;

    @Column(name = "ai_gender", length = 16)
    private String aiGender;

    @Column(name = "ai_title", length = 128)
    private String aiTitle;

    @Column(name = "ai_personality", columnDefinition = "TEXT")
    private String aiPersonality;

    @Column(name = "ai_relation", columnDefinition = "TEXT")
    private String aiRelation;

    @Column(name = "player_name", nullable = false, length = 64)
    private String playerName;

    @Column(name = "player_gender", length = 16)
    private String playerGender;

    @Column(name = "player_title", length = 128)
    private String playerTitle;

    @Column(name = "player_personality", columnDefinition = "TEXT")
    private String playerPersonality;

    @Column(name = "player_relation", columnDefinition = "TEXT")
    private String playerRelation;

    @Column(name = "scene", columnDefinition = "TEXT")
    private String scene;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
