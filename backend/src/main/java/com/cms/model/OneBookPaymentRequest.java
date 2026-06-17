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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "onebook_payment_requests")
@EntityListeners(AuditingEntityListener.class)
public class OneBookPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 60)
    private String referenceId;

    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_table", length = 100)
    private String entityTable;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_account")
    private String recipientAccount;

    @Column(name = "recipient_ifsc", length = 20)
    private String recipientIfsc;

    @Column(name = "recipient_bank_name", length = 150)
    private String recipientBankName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "onebook_txn_id")
    private String onebookTxnId;

    @Column(name = "onebook_status", length = 50)
    private String onebookStatus;

    @Column(name = "onebook_paid_date")
    private LocalDate onebookPaidDate;

    @Column(name = "onebook_payment_mode", length = 50)
    private String onebookPaymentMode;

    @Column(name = "onebook_remarks", columnDefinition = "TEXT")
    private String onebookRemarks;

    @Column(name = "onebook_raw_response", columnDefinition = "jsonb")
    private String onebookRawResponse;

    @Column(name = "request_metadata", columnDefinition = "jsonb")
    private String requestMetadata;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OneBookPaymentRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getEntityTable() { return entityTable; }
    public void setEntityTable(String entityTable) { this.entityTable = entityTable; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientAccount() { return recipientAccount; }
    public void setRecipientAccount(String recipientAccount) { this.recipientAccount = recipientAccount; }
    public String getRecipientIfsc() { return recipientIfsc; }
    public void setRecipientIfsc(String recipientIfsc) { this.recipientIfsc = recipientIfsc; }
    public String getRecipientBankName() { return recipientBankName; }
    public void setRecipientBankName(String recipientBankName) { this.recipientBankName = recipientBankName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getTransmittedAt() { return transmittedAt; }
    public void setTransmittedAt(Instant transmittedAt) { this.transmittedAt = transmittedAt; }
    public String getOnebookTxnId() { return onebookTxnId; }
    public void setOnebookTxnId(String onebookTxnId) { this.onebookTxnId = onebookTxnId; }
    public String getOnebookStatus() { return onebookStatus; }
    public void setOnebookStatus(String onebookStatus) { this.onebookStatus = onebookStatus; }
    public LocalDate getOnebookPaidDate() { return onebookPaidDate; }
    public void setOnebookPaidDate(LocalDate onebookPaidDate) { this.onebookPaidDate = onebookPaidDate; }
    public String getOnebookPaymentMode() { return onebookPaymentMode; }
    public void setOnebookPaymentMode(String onebookPaymentMode) { this.onebookPaymentMode = onebookPaymentMode; }
    public String getOnebookRemarks() { return onebookRemarks; }
    public void setOnebookRemarks(String onebookRemarks) { this.onebookRemarks = onebookRemarks; }
    public String getOnebookRawResponse() { return onebookRawResponse; }
    public void setOnebookRawResponse(String onebookRawResponse) { this.onebookRawResponse = onebookRawResponse; }
    public String getRequestMetadata() { return requestMetadata; }
    public void setRequestMetadata(String requestMetadata) { this.requestMetadata = requestMetadata; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
