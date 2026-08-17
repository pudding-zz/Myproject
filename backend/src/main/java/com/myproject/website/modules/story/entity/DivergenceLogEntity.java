package com.myproject.website.modules.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 偏离记录：原著本应… → 现在变成…
 */
@Getter
@Setter
@Entity
@Table(name = "divergence_logs")
public class DivergenceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_base_id", nullable = false)
    private Long storyBaseId;

    @Column(name = "canon_node_id")
    private Long canonNodeId;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "new_text", nullable = false, columnDefinition = "TEXT")
    private String newText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
