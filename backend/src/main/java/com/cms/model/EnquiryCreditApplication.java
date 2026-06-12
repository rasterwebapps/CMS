package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "enquiry_credit_applications")
public class EnquiryCreditApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_fee_id", nullable = false)
    private SemesterFee semesterFee;

    @Column(name = "amount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountApplied;

    /** The pre-enrollment payment receipt(s) that are the source of this credit. */
    @Column(name = "receipt_number", nullable = false, length = 100)
    private String receiptNumber;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    public EnquiryCreditApplication() {}

    public EnquiryCreditApplication(Enquiry enquiry, Student student, SemesterFee semesterFee,
                                     BigDecimal amountApplied, String receiptNumber, Instant appliedAt) {
        this.enquiry = enquiry;
        this.student = student;
        this.semesterFee = semesterFee;
        this.amountApplied = amountApplied;
        this.receiptNumber = receiptNumber;
        this.appliedAt = appliedAt;
    }

    public Long getId() { return id; }
    public Enquiry getEnquiry() { return enquiry; }
    public Student getStudent() { return student; }
    public SemesterFee getSemesterFee() { return semesterFee; }
    public BigDecimal getAmountApplied() { return amountApplied; }
    public String getReceiptNumber() { return receiptNumber; }
    public Instant getAppliedAt() { return appliedAt; }
}
