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
 * 剧情底本（对外用语，勿称「小说圣经」）
 */
@Getter
@Setter
@Entity
@Table(name = "story_bases")
public class StoryBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 128)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String background;

    /**
     * DRAFT / CONFIRMED
     */
    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "disclaimer", length = 512)
    private String disclaimer = "非官方剧情底本，仅私人娱乐，情节可能不准。";

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
