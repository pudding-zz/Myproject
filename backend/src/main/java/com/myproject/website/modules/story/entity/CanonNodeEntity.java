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
 * 原著节点：某时某地本该发生的大事件
 */
@Getter
@Setter
@Entity
@Table(name = "canon_nodes")
public class CanonNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_base_id", nullable = false)
    private Long storyBaseId;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "time_label", length = 128)
    private String timeLabel;

    @Column(length = 128)
    private String place;

    @Column(name = "original_plot", nullable = false, columnDefinition = "TEXT")
    private String originalPlot;

    /**
     * PENDING / CHANGED / SKIPPED
     */
    @Column(nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "changed_plot", columnDefinition = "TEXT")
    private String changedPlot;

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
