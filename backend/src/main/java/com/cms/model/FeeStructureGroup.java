package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;

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

/**
 * Groups fee structure items by 3 admission dimensions:
 * quota × feeState × gender.
 * Day scholar vs hosteler cost is implicit: HOSTEL_FEE row = hosteler surcharge.
 *
 * Unique key: (program, academicYear, course, quota, feeState, gender).
 * One FeeStructureGroup → many FeeStructure items (one per FeeType).
 */
@Entity
@Table(
    name = "fee_structure_groups",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_fee_structure_group",
        columnNames = {"program_id", "academic_year_id", "course_id", "quota", "fee_state_id", "gender"}
    )
)
@EntityListeners(AuditingEntityListener.class)
public class FeeStructureGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionQuota quota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_state_id", nullable = false)
    private FeeState feeState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FeeStructureGroup() {}

    public FeeStructureGroup(Program program, AcademicYear academicYear, Course course,
                              AdmissionQuota quota, FeeState feeState, Gender gender) {
        this.program = program;
        this.academicYear = academicYear;
        this.course = course;
        this.quota = quota;
        this.feeState = feeState;
        this.gender = gender;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }

    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public AdmissionQuota getQuota() { return quota; }
    public void setQuota(AdmissionQuota quota) { this.quota = quota; }

    public FeeState getFeeState() { return feeState; }
    public void setFeeState(FeeState feeState) { this.feeState = feeState; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
