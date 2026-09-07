package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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

/**
 * A recurring off-campus clinical shift window (e.g. "Shift A — Morning Clinical", 7am start).
 * Several {@link Batch} rows — each with its own existing {@code lab}/{@code clinicalVenue} —
 * link to the same group when they run clinical in parallel at different venues under the same
 * shift; the group's students reconvene into one shared theory class captured by
 * {@link ClinicalShiftTheoryBlock}. The clinical block's end time is derived at read time from
 * {@link CourseOffering#getClinicalShiftDurationMinutes()}, not stored redundantly here.
 */
@Entity
@Table(name = "clinical_shift_groups")
@EntityListeners(AuditingEntityListener.class)
public class ClinicalShiftGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_section_id")
    private CohortSection cohortSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "clinical_start_time", nullable = false)
    private LocalTime clinicalStartTime;

    /** Both null (the common case) means this group recurs for the whole term -- but only as an
     *  upper bound: {@code TimetableSkeletonService#toClinicalShiftHours} and {@code
     *  ClinicalShiftOccurrenceService#generateForDate} both additionally cap the group's real
     *  effective run at however many weekly duty-length occurrences the offering's own curriculum
     *  Clinical hours actually need (see {@code CurriculumHoursCalculator#weeksNeededFor}), and
     *  that auto-derived cap always wins if it's tighter than this date range. Both non-null bounds
     *  it to a real sub-window (e.g. a 4-week internship block) that's still further capped the
     *  same way if it would itself overshoot the hours requirement. Neither is honored by the
     *  Skeleton Builder grid, which has no per-week template variation (see V419 migration) -- a
     *  bounded/capped group still blocks its whole-term grid slot even in weeks it no longer
     *  actually runs. */
    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ClinicalShiftGroup() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public void setCourseOffering(CourseOffering courseOffering) {
        this.courseOffering = courseOffering;
    }

    public CohortSection getCohortSection() {
        return cohortSection;
    }

    public void setCohortSection(CohortSection cohortSection) {
        this.cohortSection = cohortSection;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getClinicalStartTime() {
        return clinicalStartTime;
    }

    public void setClinicalStartTime(LocalTime clinicalStartTime) {
        this.clinicalStartTime = clinicalStartTime;
    }

    public LocalDate getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public void setEffectiveStartDate(LocalDate effectiveStartDate) {
        this.effectiveStartDate = effectiveStartDate;
    }

    public LocalDate getEffectiveEndDate() {
        return effectiveEndDate;
    }

    public void setEffectiveEndDate(LocalDate effectiveEndDate) {
        this.effectiveEndDate = effectiveEndDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
