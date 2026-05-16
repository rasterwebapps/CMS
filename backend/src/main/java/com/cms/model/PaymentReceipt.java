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
@Table(name = "payment_receipts")
@EntityListeners(AuditingEntityListener.class)
public class PaymentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @Column(name = "payer_type", nullable = false, length = 10)
    private String payerType; // STUDENT | ENQUIRY

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "payer_identifier", length = 50)
    private String payerIdentifier; // roll number for students, null for enquiries

    @Column(name = "admission_number", length = 20)
    private String admissionNumber;

    @Column(name = "program_name", length = 255)
    private String programName;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_mode", nullable = false, length = 30)
    private String paymentMode;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "installments_covered", columnDefinition = "TEXT")
    private String installmentsCovered;

    @Column(name = "collected_by", length = 100)
    private String collectedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PaymentReceipt() {}

    public PaymentReceipt(String receiptNumber, String payerType, Long payerId,
                           String payerName, String payerIdentifier, String programName,
                           BigDecimal amountPaid, LocalDate paymentDate, String paymentMode,
                           String transactionReference, String remarks,
                           String installmentsCovered, String collectedBy) {
        this.receiptNumber = receiptNumber;
        this.payerType = payerType;
        this.payerId = payerId;
        this.payerName = payerName;
        this.payerIdentifier = payerIdentifier;
        this.programName = programName;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.remarks = remarks;
        this.installmentsCovered = installmentsCovered;
        this.collectedBy = collectedBy;
    }

    public PaymentReceipt(String receiptNumber, String payerType, Long payerId,
                           String payerName, String payerIdentifier, String admissionNumber, String programName,
                           BigDecimal amountPaid, LocalDate paymentDate, String paymentMode,
                           String transactionReference, String remarks,
                           String installmentsCovered, String collectedBy) {
        this(receiptNumber, payerType, payerId, payerName, payerIdentifier, programName,
            amountPaid, paymentDate, paymentMode, transactionReference, remarks, installmentsCovered, collectedBy);
        this.admissionNumber = admissionNumber;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getPayerType() { return payerType; }
    public void setPayerType(String payerType) { this.payerType = payerType; }

    public Long getPayerId() { return payerId; }
    public void setPayerId(Long payerId) { this.payerId = payerId; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public String getPayerIdentifier() { return payerIdentifier; }
    public void setPayerIdentifier(String payerIdentifier) { this.payerIdentifier = payerIdentifier; }

    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getInstallmentsCovered() { return installmentsCovered; }
    public void setInstallmentsCovered(String installmentsCovered) { this.installmentsCovered = installmentsCovered; }

    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

