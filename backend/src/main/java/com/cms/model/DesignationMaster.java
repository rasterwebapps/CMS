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
    @Column(name = "default_weekly_teaching_sessions")
    private Integer defaultWeeklyTeachingSessions;

    /** Same override precedence as {@link #defaultWeeklyTeachingSessions}, but feeds the daily hard
     *  cap ({@code timetable.faculty_max_daily_sessions}) instead of the advisory weekly report. */
    @Column(name = "default_daily_teaching_sessions")
    private Integer defaultDailyTeachingSessions;

    /** Same override precedence as {@link #defaultWeeklyTeachingSessions}, but feeds the continuous
     *  (unbroken run) hard cap ({@code timetable.faculty_max_continuous_sessions}). */
    @Column(name = "default_continuous_teaching_sessions")
    private Integer defaultContinuousTeachingSessions;

    /** Advisory floor counterpart to {@link #defaultWeeklyTeachingSessions} -- a faculty member
     *  below this many sessions/week shows as under-loaded (Faculty List, Assign Faculty, the
     *  Faculty Workload report). Never a hard block, unlike the three maximums above: refusing to
     *  place a session because it would leave someone under a floor makes no sense the way
     *  refusing one that pushes someone over a ceiling does. Same per-faculty-override-then-
     *  designation-default-then-institution-wide ({@code timetable.faculty_min_weekly_sessions})
     *  precedence. */
    @Column(name = "default_min_weekly_sessions")
    private Integer defaultMinWeeklySessions;

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

    public Integer getDefaultWeeklyTeachingSessions() { return defaultWeeklyTeachingSessions; }
    public void setDefaultWeeklyTeachingSessions(Integer defaultWeeklyTeachingSessions) { this.defaultWeeklyTeachingSessions = defaultWeeklyTeachingSessions; }

    public Integer getDefaultDailyTeachingSessions() { return defaultDailyTeachingSessions; }
    public void setDefaultDailyTeachingSessions(Integer defaultDailyTeachingSessions) { this.defaultDailyTeachingSessions = defaultDailyTeachingSessions; }

    public Integer getDefaultContinuousTeachingSessions() { return defaultContinuousTeachingSessions; }
    public void setDefaultContinuousTeachingSessions(Integer defaultContinuousTeachingSessions) { this.defaultContinuousTeachingSessions = defaultContinuousTeachingSessions; }

    public Integer getDefaultMinWeeklySessions() { return defaultMinWeeklySessions; }
    public void setDefaultMinWeeklySessions(Integer defaultMinWeeklySessions) { this.defaultMinWeeklySessions = defaultMinWeeklySessions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
