package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.DisbursementFrequency;
import com.cms.model.enums.ScholarshipStatus;

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
@Table(name = "student_scholarships")
@EntityListeners(AuditingEntityListener.class)
public class StudentScholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scholarship_type_id", nullable = false)
    private ScholarshipType scholarshipType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate = LocalDate.now();

    @Column(name = "application_remarks", columnDefinition = "TEXT")
    private String applicationRemarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScholarshipStatus status = ScholarshipStatus.PENDING;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_frequency", length = 20)
    private DisbursementFrequency disbursementFrequency;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_till")
    private LocalDate validTill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_from_id")
    private StudentScholarship renewedFrom;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ── Govt sanction tracking (for GOVT_PORTAL scheme types) ─────────────────
    /** Sanction number issued by the govt portal (NSP / ePass TN etc). */
    @Column(name = "govt_sanction_number", length = 50)
    private String govtSanctionNumber;

    /** Date the govt sanctioned the scholarship in their portal. */
    @Column(name = "sanction_date")
    private LocalDate sanctionDate;

    /** Username of the staff member who recorded the sanction. */
    @Column(name = "sanctioned_by", length = 100)
    private String sanctionedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public ScholarshipType getScholarshipType() { return scholarshipType; }
    public void setScholarshipType(ScholarshipType scholarshipType) { this.scholarshipType = scholarshipType; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    public String getApplicationRemarks() { return applicationRemarks; }
    public void setApplicationRemarks(String applicationRemarks) { this.applicationRemarks = applicationRemarks; }
    public ScholarshipStatus getStatus() { return status; }
    public void setStatus(ScholarshipStatus status) { this.status = status; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public DisbursementFrequency getDisbursementFrequency() { return disbursementFrequency; }
    public void setDisbursementFrequency(DisbursementFrequency disbursementFrequency) { this.disbursementFrequency = disbursementFrequency; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTill() { return validTill; }
    public void setValidTill(LocalDate validTill) { this.validTill = validTill; }
    public StudentScholarship getRenewedFrom() { return renewedFrom; }
    public void setRenewedFrom(StudentScholarship renewedFrom) { this.renewedFrom = renewedFrom; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getGovtSanctionNumber() { return govtSanctionNumber; }
    public void setGovtSanctionNumber(String govtSanctionNumber) { this.govtSanctionNumber = govtSanctionNumber; }
    public LocalDate getSanctionDate() { return sanctionDate; }
    public void setSanctionDate(LocalDate sanctionDate) { this.sanctionDate = sanctionDate; }
    public String getSanctionedBy() { return sanctionedBy; }
    public void setSanctionedBy(String sanctionedBy) { this.sanctionedBy = sanctionedBy; }
}

