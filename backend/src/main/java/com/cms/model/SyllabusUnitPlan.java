package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A frozen "planned completion date" for one {@link SyllabusUnit} within one {@link
 *  CourseOffering}, computed once by {@code PortionBlueprintService.generateBlueprint} from that
 *  offering's real timetable occurrences. Per-offering (not per-curriculum) since pacing depends
 *  on that offering's actual placed sessions, even though the unit list itself is shared
 *  curriculum-level data. This is the fixed reference line "Planned vs Actual/Projected" variance
 *  is measured against -- it is never mutated after generation except by a full regenerate
 *  (delete-then-reinsert every row for the offering), so a later holiday/block never silently
 *  moves it. */
@Entity
@Table(name = "syllabus_unit_plan",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_offering_id", "syllabus_unit_id"}))
@EntityListeners(AuditingEntityListener.class)
public class SyllabusUnitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_unit_id", nullable = false)
    private SyllabusUnit syllabusUnit;

    @Column(name = "planned_completion_date", nullable = false)
    private LocalDate plannedCompletionDate;

    @Column(name = "planned_cumulative_hours", nullable = false)
    private Integer plannedCumulativeHours;

    @Column(name = "sequence_index", nullable = false)
    private Integer sequenceIndex;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SyllabusUnitPlan() {
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

    public SyllabusUnit getSyllabusUnit() {
        return syllabusUnit;
    }

    public void setSyllabusUnit(SyllabusUnit syllabusUnit) {
        this.syllabusUnit = syllabusUnit;
    }

    public LocalDate getPlannedCompletionDate() {
        return plannedCompletionDate;
    }

    public void setPlannedCompletionDate(LocalDate plannedCompletionDate) {
        this.plannedCompletionDate = plannedCompletionDate;
    }

    public Integer getPlannedCumulativeHours() {
        return plannedCumulativeHours;
    }

    public void setPlannedCumulativeHours(Integer plannedCumulativeHours) {
        this.plannedCumulativeHours = plannedCumulativeHours;
    }

    public Integer getSequenceIndex() {
        return sequenceIndex;
    }

    public void setSequenceIndex(Integer sequenceIndex) {
        this.sequenceIndex = sequenceIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
