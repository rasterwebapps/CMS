package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "enquiries")
@EntityListeners(AuditingEntityListener.class)
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "enquiry_date", nullable = false)
    private LocalDate enquiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_type")
    private StudentType studentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_quota", length = 20)
    private AdmissionQuota admissionQuota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_state_id")
    private FeeState feeState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_type_id", nullable = false)
    private ReferralType referralType;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "fee_discussed_amount", precision = 10, scale = 2)
    private BigDecimal feeDiscussedAmount;

    @Column(name = "final_calculated_fee", precision = 12, scale = 2)
    private BigDecimal finalCalculatedFee;

    // ── Commission tracking (decoupled from student fee) ─────────────────────
    /**
     * Commission resolved server-side at create / update time.
     *  - If the linked agent has a non-null, &gt; 0 {@code commissionAmount} → use it.
     *  - Else if the referral type {@code hasCommission} → use the referral type's amount.
     *  - Else 0.
     * Independent of the fee charged to the student.
     */
    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_source", length = 20)
    private CommissionSource commissionSource;

    /** Total amount already paid out to the agent / referral source. */
    @Column(name = "commission_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionPaidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_payment_status", nullable = false, length = 20)
    private CommissionPaymentStatus commissionPaymentStatus = CommissionPaymentStatus.NOT_APPLICABLE;

    // Year-wise guideline fees stored as JSON
    @Column(name = "year_wise_fees", columnDefinition = "TEXT")
    private String yearWiseFees;

    // Term-wise fee breakdown stored as JSON (set at fee finalization)
    @Column(name = "term_wise_fees", columnDefinition = "TEXT")
    private String semesterWiseFees;

    // Admin fee finalization fields
    @Column(name = "finalized_total_fee", precision = 12, scale = 2)
    private BigDecimal finalizedTotalFee;

    @Column(name = "finalized_discount_amount", precision = 12, scale = 2)
    private BigDecimal finalizedDiscountAmount;

    @Column(name = "finalized_discount_reason")
    private String finalizedDiscountReason;

    @Column(name = "finalized_net_fee", precision = 12, scale = 2)
    private BigDecimal finalizedNetFee;

    @Column(name = "finalized_by")
    private String finalizedBy;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    // ── Location ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private LocationCountry country;
    private String state;
    private String district;

    // ── Referring person (only one FK will be non-null per enquiry) ───────────
    @Column(name = "referred_student_id")
    private Long referredStudentId;

    @Column(name = "referred_faculty_id")
    private Long referredFacultyId;

    @Column(name = "referred_staff_name")
    private String referredStaffName;

    @Column(name = "converted_student_id")
    private Long convertedStudentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Enquiry() {
    }

    public Enquiry(String name, String email, String phone, Program program,
                   LocalDate enquiryDate, ReferralType referralType, EnquiryStatus status) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.program = program;
        this.enquiryDate = enquiryDate;
        this.referralType = referralType;
        this.status = status;
    }

    public String getFullName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
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

    public LocalDate getEnquiryDate() {
        return enquiryDate;
    }

    public void setEnquiryDate(LocalDate enquiryDate) {
        this.enquiryDate = enquiryDate;
    }

    public EnquiryStatus getStatus() {
        return status;
    }

    public void setStatus(EnquiryStatus status) {
        this.status = status;
    }

    public StudentType getStudentType() {
        return studentType;
    }

    public void setStudentType(StudentType studentType) {
        this.studentType = studentType;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public ReferralType getReferralType() {
        return referralType;
    }

    public void setReferralType(ReferralType referralType) {
        this.referralType = referralType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getFeeDiscussedAmount() {
        return feeDiscussedAmount;
    }

    public void setFeeDiscussedAmount(BigDecimal feeDiscussedAmount) {
        this.feeDiscussedAmount = feeDiscussedAmount;
    }

    public BigDecimal getFinalCalculatedFee() {
        return finalCalculatedFee;
    }

    public void setFinalCalculatedFee(BigDecimal finalCalculatedFee) {
        this.finalCalculatedFee = finalCalculatedFee;
    }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public CommissionSource getCommissionSource() { return commissionSource; }
    public void setCommissionSource(CommissionSource commissionSource) { this.commissionSource = commissionSource; }

    public BigDecimal getCommissionPaidAmount() { return commissionPaidAmount; }
    public void setCommissionPaidAmount(BigDecimal commissionPaidAmount) {
        this.commissionPaidAmount = commissionPaidAmount != null ? commissionPaidAmount : BigDecimal.ZERO;
    }

    public CommissionPaymentStatus getCommissionPaymentStatus() { return commissionPaymentStatus; }
    public void setCommissionPaymentStatus(CommissionPaymentStatus commissionPaymentStatus) {
        this.commissionPaymentStatus = commissionPaymentStatus != null
            ? commissionPaymentStatus
            : CommissionPaymentStatus.NOT_APPLICABLE;
    }

    public String getYearWiseFees() {
        return yearWiseFees;
    }

    public void setYearWiseFees(String yearWiseFees) {
        this.yearWiseFees = yearWiseFees;
    }

    public String getSemesterWiseFees() {
        return semesterWiseFees;
    }

    public void setSemesterWiseFees(String semesterWiseFees) {
        this.semesterWiseFees = semesterWiseFees;
    }

    public BigDecimal getFinalizedTotalFee() {
        return finalizedTotalFee;
    }

    public void setFinalizedTotalFee(BigDecimal finalizedTotalFee) {
        this.finalizedTotalFee = finalizedTotalFee;
    }

    public BigDecimal getFinalizedDiscountAmount() {
        return finalizedDiscountAmount;
    }

    public void setFinalizedDiscountAmount(BigDecimal finalizedDiscountAmount) {
        this.finalizedDiscountAmount = finalizedDiscountAmount;
    }

    public String getFinalizedDiscountReason() {
        return finalizedDiscountReason;
    }

    public void setFinalizedDiscountReason(String finalizedDiscountReason) {
        this.finalizedDiscountReason = finalizedDiscountReason;
    }

    public BigDecimal getFinalizedNetFee() {
        return finalizedNetFee;
    }

    public void setFinalizedNetFee(BigDecimal finalizedNetFee) {
        this.finalizedNetFee = finalizedNetFee;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(String finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Instant finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public LocationCountry getCountry() { return country; }
    public void setCountry(LocationCountry country) { this.country = country; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public Long getReferredStudentId() { return referredStudentId; }
    public void setReferredStudentId(Long referredStudentId) { this.referredStudentId = referredStudentId; }

    public Long getReferredFacultyId() { return referredFacultyId; }
    public void setReferredFacultyId(Long referredFacultyId) { this.referredFacultyId = referredFacultyId; }

    public String getReferredStaffName() { return referredStaffName; }
    public void setReferredStaffName(String referredStaffName) { this.referredStaffName = referredStaffName; }

    public Long getConvertedStudentId() {
        return convertedStudentId;
    }

    public void setConvertedStudentId(Long convertedStudentId) {
        this.convertedStudentId = convertedStudentId;
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

    public AdmissionQuota getAdmissionQuota() { return admissionQuota; }
    public void setAdmissionQuota(AdmissionQuota admissionQuota) { this.admissionQuota = admissionQuota; }

    public FeeState getFeeState() { return feeState; }
    public void setFeeState(FeeState feeState) { this.feeState = feeState; }
}
