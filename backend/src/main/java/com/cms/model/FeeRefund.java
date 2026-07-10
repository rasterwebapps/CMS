package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fee_refunds")
@EntityListeners(AuditingEntityListener.class)
public class FeeRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Generated on APPROVED; null while PENDING. */
    @Column(name = "refund_number", unique = true, length = 50)
    private String refundNumber;

    @Column(name = "original_receipt_number", nullable = false, length = 50)
    private String originalReceiptNumber;

    /** STUDENT | ENQUIRY — determines which entity this refund belongs to. */
    @Column(name = "entity_type", nullable = false, length = 10)
    private String entityType = "STUDENT";

    /** Set when entity_type = STUDENT; null for ENQUIRY. */
    @Column(name = "student_id")
    private Long studentId;

    /** Set when entity_type = ENQUIRY; null for STUDENT. */
    @Column(name = "enquiry_id")
    private Long enquiryId;

    /** Display name — student full name or enquiry person name. */
    @Column(name = "student_name", length = 255)
    private String studentName;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "admission_number", length = 20)
    private String admissionNumber;

    @Column(name = "program_name", length = 255)
    private String programName;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** PENDING | APPROVED | REJECTED */
    @Column(nullable = false, length = 20)
    private String status;

    /** MANUAL (staff-initiated) | AUTO_EXCESS (system-generated for excess-over-outstanding bank payments). */
    @Column(nullable = false, length = 20)
    private String source = "MANUAL";

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    // ── Set on APPROVED ───────────────────────────────────────────────────────

    @Column(name = "payment_mode", length = 30)
    private String paymentMode;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    // ── Set on REJECTED ───────────────────────────────────────────────────────

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FeeRefund() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRefundNumber() { return refundNumber; }
    public void setRefundNumber(String refundNumber) { this.refundNumber = refundNumber; }

    public String getOriginalReceiptNumber() { return originalReceiptNumber; }
    public void setOriginalReceiptNumber(String v) { this.originalReceiptNumber = v; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getEnquiryId() { return enquiryId; }
    public void setEnquiryId(Long enquiryId) { this.enquiryId = enquiryId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
