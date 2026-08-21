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
 * Per-section Theory faculty override for a {@link CourseOffering} -- sparse by design, a row only
 * exists for a {@link CohortSection} whose Theory delivery diverges from the offering's own
 * primary {@code facultyId}. A section with no row here falls back to the offering's primary,
 * matching exactly how {@link Batch#getCoordinatorFaculty()} already works for LAB/CLINICAL.
 * Advisory/accounting-only, same as batches -- feeds {@code TimetableGlobalAutoScheduleService}'s
 * capacity math, never Skeleton Builder placement or Staffing.
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
    @JoinColumn(name = "cohort_section_id", nullable = false)
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

    public CourseOfferingSectionFaculty(CourseOffering courseOffering, CohortSection cohortSection, Faculty faculty) {
        this.courseOffering = courseOffering;
        this.cohortSection = cohortSection;
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
