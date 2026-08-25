package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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

/**
 * Per-(offering, cohort) faculty assignment -- authoritative for placement. {@link #cohortSection}
 * is set when that cohort's Theory delivery has split into room-allocated sections (one row per
 * active {@link CohortSection}, each independently assignable); {@code null} means the whole
 * cohort, no split -- exactly one such row per (offering, cohort). {@link #cohort} is always set
 * regardless, since a single {@link CourseOffering} can be shared by more than one cohort (same
 * curriculum version, e.g. two admission-year batches) and each is assigned independently -- there
 * is no longer any offering-wide "primary" faculty to fall back to. Mirrors exactly how {@link
 * Batch#getCoordinatorFaculty()} already works for LAB/CLINICAL parallel groups.
 */
@Entity
@Table(name = "course_offering_section_faculty",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_offering_id", "cohort_section_id"}))
@EntityListeners(AuditingEntityListener.class)
public class CourseOfferingSectionFaculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    /** Null means this row covers the whole cohort (no active section split) -- see class javadoc.
     *  Exactly one whole-cohort row (per offering) is allowed per cohort, enforced by a partial
     *  unique index (course_offering_id, cohort_id) WHERE cohort_section_id IS NULL, since a plain
     *  multi-column UNIQUE constraint would treat repeated NULLs as distinct. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_section_id")
    private CohortSection cohortSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CourseOfferingSectionFaculty() {
    }

    /** Section-scoped row constructor -- derives {@code cohort} from the section itself so callers
     *  never have to resolve/pass it separately. */
    public CourseOfferingSectionFaculty(CourseOffering courseOffering, CohortSection cohortSection, Faculty faculty) {
        this.courseOffering = courseOffering;
        this.cohortSection = cohortSection;
        this.cohort = cohortSection.getCohortRoomAllocation().getCohort();
        this.faculty = faculty;
    }

    /** Whole-cohort row constructor (no section split). */
    public CourseOfferingSectionFaculty(CourseOffering courseOffering, Cohort cohort, Faculty faculty) {
        this.courseOffering = courseOffering;
        this.cohort = cohort;
        this.faculty = faculty;
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

    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public CohortSection getCohortSection() {
        return cohortSection;
    }

    public void setCohortSection(CohortSection cohortSection) {
        this.cohortSection = cohortSection;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
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
