package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.QualificationType;

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

@Entity
@Table(name = "academic_qualifications")
@EntityListeners(AuditingEntityListener.class)
public class AcademicQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_type", nullable = false)
    private QualificationType qualificationType;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "major_subject")
    private String majorSubject;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "month_and_year_of_passing")
    private String monthAndYearOfPassing;

    @Column(name = "university_or_board")
    private String universityOrBoard;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AcademicQualification() {
    }

    public AcademicQualification(Admission admission, QualificationType qualificationType,
                                  String schoolName, String majorSubject, Integer totalMarks,
                                  BigDecimal percentage, String monthAndYearOfPassing,
                                  String universityOrBoard) {
        this.admission = admission;
        this.qualificationType = qualificationType;
        this.schoolName = schoolName;
        this.majorSubject = majorSubject;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.monthAndYearOfPassing = monthAndYearOfPassing;
        this.universityOrBoard = universityOrBoard;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public QualificationType getQualificationType() {
        return qualificationType;
    }

    public void setQualificationType(QualificationType qualificationType) {
        this.qualificationType = qualificationType;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getMajorSubject() {
        return majorSubject;
    }

    public void setMajorSubject(String majorSubject) {
        this.majorSubject = majorSubject;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getMonthAndYearOfPassing() {
        return monthAndYearOfPassing;
    }

    public void setMonthAndYearOfPassing(String monthAndYearOfPassing) {
        this.monthAndYearOfPassing = monthAndYearOfPassing;
    }

    public String getUniversityOrBoard() {
        return universityOrBoard;
    }

    public void setUniversityOrBoard(String universityOrBoard) {
        this.universityOrBoard = universityOrBoard;
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
