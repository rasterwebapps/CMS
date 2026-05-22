package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.CohortStatus;
import jakarta.persistence.Transient;

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

@Entity
@Table(name = "cohorts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "admission_academic_year_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Cohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_academic_year_id", nullable = false)
    private AcademicYear admissionAcademicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_graduation_academic_year_id")
    private AcademicYear expectedGraduationAcademicYear;

    @Column(name = "cohort_code", nullable = false, unique = true, length = 50)
    private String cohortCode;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "management_seats")
    private Integer managementSeats;

    @Column(name = "counselling_seats")
    private Integer counsellingSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CohortStatus status = CohortStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Cohort() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    /** Delegation convenience — returns the Program via the Course chain. */
    @Transient
    public Program getProgram() {
        return course != null ? course.getProgram() : null;
    }

    public AcademicYear getAdmissionAcademicYear() {
        return admissionAcademicYear;
    }

    public void setAdmissionAcademicYear(AcademicYear admissionAcademicYear) {
        this.admissionAcademicYear = admissionAcademicYear;
    }

    public AcademicYear getExpectedGraduationAcademicYear() {
        return expectedGraduationAcademicYear;
    }

    public void setExpectedGraduationAcademicYear(AcademicYear expectedGraduationAcademicYear) {
        this.expectedGraduationAcademicYear = expectedGraduationAcademicYear;
    }

    public String getCohortCode() {
        return cohortCode;
    }

    public void setCohortCode(String cohortCode) {
        this.cohortCode = cohortCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getManagementSeats() { return managementSeats; }
    public void setManagementSeats(Integer managementSeats) { this.managementSeats = managementSeats; }
    public Integer getCounsellingSeats() { return counsellingSeats; }
    public void setCounsellingSeats(Integer counsellingSeats) { this.counsellingSeats = counsellingSeats; }

    public CohortStatus getStatus() {
        return status;
    }

    public void setStatus(CohortStatus status) {
        this.status = status;
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
