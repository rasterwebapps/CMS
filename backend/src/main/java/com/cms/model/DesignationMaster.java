package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "designations")
@EntityListeners(AuditingEntityListener.class)
public class DesignationMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Advisory-only default for the faculty capacity-planning report; a per-Faculty override
     *  wins over this when set. Null means unconfigured, not zero. */
    @Column(name = "default_weekly_teaching_hours")
    private Integer defaultWeeklyTeachingHours;

    /** Same override precedence as {@link #defaultWeeklyTeachingHours}, but feeds the daily hard
     *  cap ({@code timetable.faculty_max_daily_hours}) instead of the advisory weekly report. */
    @Column(name = "default_daily_teaching_hours")
    private Integer defaultDailyTeachingHours;

    /** Same override precedence as {@link #defaultWeeklyTeachingHours}, but feeds the continuous
     *  (unbroken run) hard cap ({@code timetable.faculty_max_continuous_hours}). */
    @Column(name = "default_continuous_teaching_hours")
    private Integer defaultContinuousTeachingHours;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DesignationMaster() {}

    public DesignationMaster(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getDefaultWeeklyTeachingHours() { return defaultWeeklyTeachingHours; }
    public void setDefaultWeeklyTeachingHours(Integer defaultWeeklyTeachingHours) { this.defaultWeeklyTeachingHours = defaultWeeklyTeachingHours; }

    public Integer getDefaultDailyTeachingHours() { return defaultDailyTeachingHours; }
    public void setDefaultDailyTeachingHours(Integer defaultDailyTeachingHours) { this.defaultDailyTeachingHours = defaultDailyTeachingHours; }

    public Integer getDefaultContinuousTeachingHours() { return defaultContinuousTeachingHours; }
    public void setDefaultContinuousTeachingHours(Integer defaultContinuousTeachingHours) { this.defaultContinuousTeachingHours = defaultContinuousTeachingHours; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
