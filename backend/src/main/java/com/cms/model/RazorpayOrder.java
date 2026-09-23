package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.RazorpayOrderStatus;

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

/** Pre-capture bookkeeping for a Razorpay order, not the money ledger itself -- once a payment is
 *  actually captured, {@link com.cms.service.RazorpayWebhookService} creates the real
 *  {@link PaymentReceipt}/{@code FeeInstallment} rows via the existing
 *  {@link com.cms.service.PaymentCollectionService#collectPayment} path, same as a staff-collected
 *  payment. This table exists so an incoming webhook's {@code razorpay_order_id} can be resolved
 *  back to a student/amount, and so {@code razorpay_payment_id}'s unique constraint detects a
 *  duplicate webhook delivery before a second receipt is ever created. */
@Entity
@Table(name = "razorpay_orders")
@EntityListeners(AuditingEntityListener.class)
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 64)
    private String razorpayOrderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RazorpayOrderStatus status = RazorpayOrderStatus.CREATED;

    @Column(name = "razorpay_payment_id", unique = true, length = 64)
    private String razorpayPaymentId;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Guardian getGuardian() { return guardian; }
    public void setGuardian(Guardian guardian) { this.guardian = guardian; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public RazorpayOrderStatus getStatus() { return status; }
    public void setStatus(RazorpayOrderStatus status) { this.status = status; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
