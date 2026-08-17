package com.myproject.website.modules.roleplay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "roleplay_health_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roleplay_health_session_day",
                columnNames = {"session_id", "day"}))
public class RoleplayHealthRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 日历日 1–31 */
    @Column(name = "day", nullable = false)
    private Integer day;

    @Column(name = "cal", nullable = false)
    private Integer cal = 0;

    @Column(name = "heart", nullable = false)
    private Integer heart = 0;

    @Column(name = "count", nullable = false)
    private Integer count = 0;

    @Column(name = "duration", nullable = false)
    private Integer duration = 0;

    /** API JSON 字段名仍为 trigger */
    @Column(name = "trigger_text", columnDefinition = "TEXT")
    private String triggerText;

    @Column(name = "scene", columnDefinition = "TEXT")
    private String scene;

    @Column(name = "thought", columnDefinition = "TEXT")
    private String thought;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (cal == null) {
            cal = 0;
        }
        if (heart == null) {
            heart = 0;
        }
        if (count == null) {
            count = 0;
        }
        if (duration == null) {
            duration = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
