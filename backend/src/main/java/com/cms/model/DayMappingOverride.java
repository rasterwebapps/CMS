package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Declares that a specific calendar date ({@link #mappedDate}) runs a DIFFERENT weekday's
 *  timetable ({@link #borrowedDayOfWeek}) than its own actual weekday -- the standard
 *  compensatory-working-day pattern ("this Saturday runs Monday's schedule"). Narrow, automatic,
 *  institution-wide: {@code mapped_date} is globally unique, one row always fully suppresses that
 *  date's own actual-weekday sessions and substitutes the borrowed weekday's instead. Resolved
 *  entirely at read-time by {@link com.cms.service.ClassScheduleOccurrenceService} (and, downstream,
 *  {@link com.cms.service.FacultyAbsenceService}/{@link com.cms.service.ResourceGridService}/
 *  attendance) -- never materializes new {@code ClassSchedule}/{@code SessionOccurrence} rows,
 *  same philosophy as the existing {@link BlockedPeriod} holiday-skip logic. */
@Entity
@Table(name = "day_mapping_overrides")
@EntityListeners(AuditingEntityListener.class)
public class DayMappingOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @Column(name = "mapped_date", nullable = false)
    private LocalDate mappedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "borrowed_day_of_week", nullable = false)
    private DayOfWeek borrowedDayOfWeek;

    @Column(nullable = false)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DayMappingOverride() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public LocalDate getMappedDate() {
        return mappedDate;
    }

    public void setMappedDate(LocalDate mappedDate) {
        this.mappedDate = mappedDate;
    }

    public DayOfWeek getBorrowedDayOfWeek() {
        return borrowedDayOfWeek;
    }

    public void setBorrowedDayOfWeek(DayOfWeek borrowedDayOfWeek) {
        this.borrowedDayOfWeek = borrowedDayOfWeek;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
