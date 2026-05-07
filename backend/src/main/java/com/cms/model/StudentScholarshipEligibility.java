package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_scholarship_eligibility")
@EntityListeners(AuditingEntityListener.class)
public class StudentScholarshipEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "is_first_graduate", nullable = false)
    private boolean firstGraduate;

    @Column(name = "is_merit_based", nullable = false)
    private boolean meritBased;

    @Column(name = "is_sports_quota", nullable = false)
    private boolean sportsQuota;

    @Column(name = "is_economically_weaker", nullable = false)
    private boolean economicallyWeaker;

    @Column(name = "annual_family_income", precision = 12, scale = 2)
    private BigDecimal annualFamilyIncome;

    @Column(name = "income_certificate_number", length = 50)
    private String incomeCertificateNumber;

    @Column(name = "income_cert_issuing_authority", length = 100)
    private String incomeCertIssuingAuthority;

    @Column(name = "income_cert_issue_date")
    private LocalDate incomeCertIssueDate;

    @Column(name = "community_certificate_number", length = 50)
    private String communityCertificateNumber;

    @Column(name = "comm_cert_issuing_authority", length = 100)
    private String commCertIssuingAuthority;

    @Column(name = "comm_cert_issue_date")
    private LocalDate commCertIssueDate;

    @Column(name = "first_graduate_certificate_number", length = 50)
    private String firstGraduateCertificateNumber;

    @Column(name = "first_grad_cert_issuing_authority", length = 100)
    private String firstGradCertIssuingAuthority;

    @Column(name = "first_grad_cert_issue_date")
    private LocalDate firstGradCertIssueDate;

    @Column(name = "father_education", length = 100)
    private String fatherEducation;

    @Column(name = "mother_education", length = 100)
    private String motherEducation;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_remarks", columnDefinition = "TEXT")
    private String verificationRemarks;

    // ── DBT (Direct Benefit Transfer) details ─────────────────────────────────
    // Required for govt scholarships (NSP, ePass TN). Money is credited directly
    // to the student's Aadhaar-linked bank account.

    /** Aadhaar number (12 digits). Stored as plain digits; UI is responsible for masking display. */
    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 15)
    private String bankIfsc;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    /** True when the bank account has been seeded with Aadhaar for DBT credit. */
    @Column(name = "dbt_linked", nullable = false)
    private boolean dbtLinked;

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
    public boolean isFirstGraduate() { return firstGraduate; }
    public void setFirstGraduate(boolean firstGraduate) { this.firstGraduate = firstGraduate; }
    public boolean isMeritBased() { return meritBased; }
    public void setMeritBased(boolean meritBased) { this.meritBased = meritBased; }
    public boolean isSportsQuota() { return sportsQuota; }
    public void setSportsQuota(boolean sportsQuota) { this.sportsQuota = sportsQuota; }
    public boolean isEconomicallyWeaker() { return economicallyWeaker; }
    public void setEconomicallyWeaker(boolean economicallyWeaker) { this.economicallyWeaker = economicallyWeaker; }
    public BigDecimal getAnnualFamilyIncome() { return annualFamilyIncome; }
    public void setAnnualFamilyIncome(BigDecimal annualFamilyIncome) { this.annualFamilyIncome = annualFamilyIncome; }
    public String getIncomeCertificateNumber() { return incomeCertificateNumber; }
    public void setIncomeCertificateNumber(String incomeCertificateNumber) { this.incomeCertificateNumber = incomeCertificateNumber; }
    public String getIncomeCertIssuingAuthority() { return incomeCertIssuingAuthority; }
    public void setIncomeCertIssuingAuthority(String incomeCertIssuingAuthority) { this.incomeCertIssuingAuthority = incomeCertIssuingAuthority; }
    public LocalDate getIncomeCertIssueDate() { return incomeCertIssueDate; }
    public void setIncomeCertIssueDate(LocalDate incomeCertIssueDate) { this.incomeCertIssueDate = incomeCertIssueDate; }
    public String getCommunityCertificateNumber() { return communityCertificateNumber; }
    public void setCommunityCertificateNumber(String communityCertificateNumber) { this.communityCertificateNumber = communityCertificateNumber; }
    public String getCommCertIssuingAuthority() { return commCertIssuingAuthority; }
    public void setCommCertIssuingAuthority(String commCertIssuingAuthority) { this.commCertIssuingAuthority = commCertIssuingAuthority; }
    public LocalDate getCommCertIssueDate() { return commCertIssueDate; }
    public void setCommCertIssueDate(LocalDate commCertIssueDate) { this.commCertIssueDate = commCertIssueDate; }
    public String getFirstGraduateCertificateNumber() { return firstGraduateCertificateNumber; }
    public void setFirstGraduateCertificateNumber(String firstGraduateCertificateNumber) { this.firstGraduateCertificateNumber = firstGraduateCertificateNumber; }
    public String getFirstGradCertIssuingAuthority() { return firstGradCertIssuingAuthority; }
    public void setFirstGradCertIssuingAuthority(String firstGradCertIssuingAuthority) { this.firstGradCertIssuingAuthority = firstGradCertIssuingAuthority; }
    public LocalDate getFirstGradCertIssueDate() { return firstGradCertIssueDate; }
    public void setFirstGradCertIssueDate(LocalDate firstGradCertIssueDate) { this.firstGradCertIssueDate = firstGradCertIssueDate; }
    public String getFatherEducation() { return fatherEducation; }
    public void setFatherEducation(String fatherEducation) { this.fatherEducation = fatherEducation; }
    public String getMotherEducation() { return motherEducation; }
    public void setMotherEducation(String motherEducation) { this.motherEducation = motherEducation; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getVerificationRemarks() { return verificationRemarks; }
    public void setVerificationRemarks(String verificationRemarks) { this.verificationRemarks = verificationRemarks; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public boolean isDbtLinked() { return dbtLinked; }
    public void setDbtLinked(boolean dbtLinked) { this.dbtLinked = dbtLinked; }
}

