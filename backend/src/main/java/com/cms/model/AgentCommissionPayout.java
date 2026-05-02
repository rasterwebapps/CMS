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

/**
 * A single commission payout made to an agent (or other referral source) for
 * an enquiry.  Multiple rows allow PARTIAL → PAID transitions.
 */
@Entity
@Table(name = "agent_commission_payouts")
@EntityListeners(AuditingEntityListener.class)
public class AgentCommissionPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    /** Nullable: enquiry may have no agent (commission via referral type only). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_date", nullable = false)
    private LocalDate payoutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "paid_by")
    private String paidBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AgentCommissionPayout() {
    }

    public AgentCommissionPayout(Enquiry enquiry, Agent agent, BigDecimal amount,
                                  LocalDate payoutDate, PaymentMode paymentMode,
                                  String transactionReference, String remarks, String paidBy) {
        this.enquiry = enquiry;
        this.agent = agent;
        this.amount = amount;
        this.payoutDate = payoutDate;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.remarks = remarks;
        this.paidBy = paidBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Enquiry getEnquiry() { return enquiry; }
    public void setEnquiry(Enquiry enquiry) { this.enquiry = enquiry; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPayoutDate() { return payoutDate; }
    public void setPayoutDate(LocalDate payoutDate) { this.payoutDate = payoutDate; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

