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

@Entity
@Table(name = "curriculum_versions")
@EntityListeners(AuditingEntityListener.class)
public class CurriculumVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /**
     * Optional narrower scope than program: when set, this version applies only to this
     * course (e.g. MSc Nursing (Adult) vs (Child), which share one Program but need
     * independent curricula). NULL means program-wide, matching pre-existing behaviour.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "version_name", nullable = false, length = 100)
    private String versionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_from_academic_year_id", nullable = false)
    private AcademicYear effectiveFromAcademicYear;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CurriculumVersion() {
    }

    public CurriculumVersion(Program program, String versionName,
                              AcademicYear effectiveFromAcademicYear, Boolean isActive) {
        this(program, null, versionName, effectiveFromAcademicYear, isActive);
    }

    public CurriculumVersion(Program program, Course course, String versionName,
                              AcademicYear effectiveFromAcademicYear, Boolean isActive) {
        this.program = program;
        this.course = course;
        this.versionName = versionName;
        this.effectiveFromAcademicYear = effectiveFromAcademicYear;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public AcademicYear getEffectiveFromAcademicYear() {
        return effectiveFromAcademicYear;
    }

    public void setEffectiveFromAcademicYear(AcademicYear effectiveFromAcademicYear) {
        this.effectiveFromAcademicYear = effectiveFromAcademicYear;
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
