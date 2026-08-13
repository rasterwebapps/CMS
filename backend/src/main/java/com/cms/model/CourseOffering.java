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

    @Column(name = "faculty_id")
    private Long facultyId;

    /** Co-instructor for this offering — same department-eligibility gate as {@link #facultyId}
     *  (see {@code CourseOfferingServiceImpl.requireEligibleFaculty}), and eligible as a substitute
     *  candidate ({@link com.cms.service.FacultyAbsenceService#findEligibleSubstitutes}). Still
     *  never gets its own {@code ClassSchedule} rows and is never directly staffed onto one — only
     *  offered as a substitute when the primary is absent. */
    @Column(name = "secondary_faculty_id")
    private Long secondaryFacultyId;

    @Column(name = "section_label", length = 50)
    private String sectionLabel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public Long getSecondaryFacultyId() {
        return secondaryFacultyId;
    }

    public void setSecondaryFacultyId(Long secondaryFacultyId) {
        this.secondaryFacultyId = secondaryFacultyId;
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public void setSectionLabel(String sectionLabel) {
        this.sectionLabel = sectionLabel;
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
