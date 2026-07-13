package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
import jakarta.persistence.UniqueConstraint;

import com.cms.model.enums.SubjectType;

@Entity
@Table(name = "curriculum_term_courses",
    uniqueConstraints = @UniqueConstraint(columnNames = {"curriculum_version_id", "term_number", "subject_id"}))
@EntityListeners(AuditingEntityListener.class)
public class CurriculumSemesterCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_version_id", nullable = false)
    private CurriculumVersion curriculumVersion;

    @Column(name = "term_number", nullable = false)
    private Integer semesterNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "theory_hours", nullable = false)
    private Integer theoryHours = 0;

    @Column(name = "lab_hours", nullable = false)
    private Integer labHours = 0;

    @Column(name = "clinical_hours", nullable = false)
    private Integer clinicalHours = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType = SubjectType.CORE;

    @Column(name = "is_elective", nullable = false)
    private Boolean isElective = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elective_group_id")
    private CurriculumElectiveGroup electiveGroup;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CurriculumSemesterCourse() {
    }

    public CurriculumSemesterCourse(CurriculumVersion curriculumVersion, Integer semesterNumber,
                                     Subject subject, Integer sortOrder) {
        this.curriculumVersion = curriculumVersion;
        this.semesterNumber = semesterNumber;
        this.subject = subject;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CurriculumVersion getCurriculumVersion() {
        return curriculumVersion;
    }

    public void setCurriculumVersion(CurriculumVersion curriculumVersion) {
        this.curriculumVersion = curriculumVersion;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getTheoryHours() {
        return theoryHours;
    }

    public void setTheoryHours(Integer theoryHours) {
        this.theoryHours = theoryHours != null ? theoryHours : 0;
    }

    public Integer getLabHours() {
        return labHours;
    }

    public void setLabHours(Integer labHours) {
        this.labHours = labHours != null ? labHours : 0;
    }

    public Integer getClinicalHours() {
        return clinicalHours;
    }

    public void setClinicalHours(Integer clinicalHours) {
        this.clinicalHours = clinicalHours != null ? clinicalHours : 0;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType != null ? subjectType : SubjectType.CORE;
    }

    public Boolean getIsElective() {
        return isElective;
    }

    public void setIsElective(Boolean isElective) {
        this.isElective = isElective != null ? isElective : false;
    }

    public CurriculumElectiveGroup getElectiveGroup() {
        return electiveGroup;
    }

    public void setElectiveGroup(CurriculumElectiveGroup electiveGroup) {
        this.electiveGroup = electiveGroup;
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
