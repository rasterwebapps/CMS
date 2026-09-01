package com.cms.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "course_offerings",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"term_instance_id", "curriculum_version_id", "subject_id", "term_number"}))
@EntityListeners(AuditingEntityListener.class)
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_version_id", nullable = false)
    private CurriculumVersion curriculumVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "term_number", nullable = false)
    private Integer semesterNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_term_course_id")
    private CurriculumSemesterCourse curriculumSemesterCourse;

    /** Admin-curated shortlist of faculty considered for this specific offering -- a further
     *  narrowing of {@code FacultyEligibility.eligibleFaculty} (Speciality match OR the subject's
     *  Eligible Faculty list), which still governs who can ever be added here. Every active {@code
     *  CourseOfferingSectionFaculty} row for this offering must always be assignable from this pool
     *  -- {@code CourseOfferingServiceImpl.updateFacultyPool} blocks removing anyone currently
     *  relied upon rather than silently orphaning their assignment. Empty means no pool has been
     *  built yet. */
    @ManyToMany
    @JoinTable(name = "course_offering_faculty_pool", joinColumns = @JoinColumn(name = "course_offering_id"),
        inverseJoinColumns = @JoinColumn(name = "faculty_id"))
    private Set<Faculty> facultyPool = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Configurable off-campus clinical shift length for this offering (OC-175) — some postings
     *  run 6h, others 8h, per real-hours-covered inspection requirements. Null means this offering
     *  has no shift-based clinical component (on-campus-only clinical stays on the existing
     *  Period-based path). Applies to every {@link ClinicalShiftGroup} under this offering. */
    @Column(name = "clinical_shift_duration_minutes")
    private Integer clinicalShiftDurationMinutes;

    /** Symmetric bus-travel buffer applied before and after every clinical block under this
     *  offering's shift groups — derives bus depart/return, doesn't store them redundantly. */
    @Column(name = "clinical_travel_buffer_minutes")
    private Integer clinicalTravelBufferMinutes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CourseOffering() {
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

    public CurriculumVersion getCurriculumVersion() {
        return curriculumVersion;
    }

    public void setCurriculumVersion(CurriculumVersion curriculumVersion) {
        this.curriculumVersion = curriculumVersion;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public CurriculumSemesterCourse getCurriculumSemesterCourse() {
        return curriculumSemesterCourse;
    }

    public void setCurriculumSemesterCourse(CurriculumSemesterCourse curriculumSemesterCourse) {
        this.curriculumSemesterCourse = curriculumSemesterCourse;
    }

    public Set<Faculty> getFacultyPool() {
        return facultyPool;
    }

    public void setFacultyPool(Set<Faculty> facultyPool) {
        this.facultyPool = facultyPool;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getClinicalShiftDurationMinutes() {
        return clinicalShiftDurationMinutes;
    }

    public void setClinicalShiftDurationMinutes(Integer clinicalShiftDurationMinutes) {
        this.clinicalShiftDurationMinutes = clinicalShiftDurationMinutes;
    }

    public Integer getClinicalTravelBufferMinutes() {
        return clinicalTravelBufferMinutes;
    }

    public void setClinicalTravelBufferMinutes(Integer clinicalTravelBufferMinutes) {
        this.clinicalTravelBufferMinutes = clinicalTravelBufferMinutes;
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
