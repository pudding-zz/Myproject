package com.myproject.website.modules.story.entity;

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

/**
 * 当前世界：穿书进行中的世界状态
 */
@Getter
@Setter
@Entity
@Table(name = "world_states")
public class WorldStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_base_id", nullable = false, unique = true)
    private Long storyBaseId;

    @Column(name = "current_time", length = 128)
    private String currentTime;

    @Column(name = "current_place", length = 128)
    private String currentPlace;

    @Column(name = "present_characters", columnDefinition = "TEXT")
    private String presentCharacters;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
