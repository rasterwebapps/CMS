package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.PaymentMode;

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
@Table(name = "term_fee_payments")
@EntityListeners(AuditingEntityListener.class)
public class TermFeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_demand_id", nullable = false)
    private FeeDemand feeDemand;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "late_fee_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFeeApplied = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 50)
    private PaymentMode paymentMode;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TermFeePayment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeeDemand getFeeDemand() {
        return feeDemand;
    }

    public void setFeeDemand(FeeDemand feeDemand) {
        this.feeDemand = feeDemand;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getLateFeeApplied() {
        return lateFeeApplied;
    }

    public void setLateFeeApplied(BigDecimal lateFeeApplied) {
        this.lateFeeApplied = lateFeeApplied;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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

    /** Computed: amountPaid + lateFeeApplied. Not persisted. */
    public BigDecimal getTotalCollected() {
        BigDecimal amount = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal late = lateFeeApplied != null ? lateFeeApplied : BigDecimal.ZERO;
        return amount.add(late);
    }
}
