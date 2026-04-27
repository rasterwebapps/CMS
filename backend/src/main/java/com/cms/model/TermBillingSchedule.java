package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "term_billing_schedules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year_id", "term_type"}))
@EntityListeners(AuditingEntityListener.class)
public class TermBillingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false, length = 10)
    private TermType termType;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "late_fee_type", nullable = false, length = 10)
    private LateFeeType lateFeeType;

    @Column(name = "late_fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFeeAmount;

    @Column(name = "grace_days", nullable = false)
    private Integer graceDays = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TermBillingSchedule() {
    }

    public TermBillingSchedule(AcademicYear academicYear, TermType termType,
                                LocalDate dueDate, LateFeeType lateFeeType,
                                BigDecimal lateFeeAmount, Integer graceDays) {
        this.academicYear = academicYear;
        this.termType = termType;
        this.dueDate = dueDate;
        this.lateFeeType = lateFeeType;
        this.lateFeeAmount = lateFeeAmount;
        this.graceDays = graceDays != null ? graceDays : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public TermType getTermType() {
        return termType;
    }

    public void setTermType(TermType termType) {
        this.termType = termType;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LateFeeType getLateFeeType() {
        return lateFeeType;
    }

    public void setLateFeeType(LateFeeType lateFeeType) {
        this.lateFeeType = lateFeeType;
    }

    public BigDecimal getLateFeeAmount() {
        return lateFeeAmount;
    }

    public void setLateFeeAmount(BigDecimal lateFeeAmount) {
        this.lateFeeAmount = lateFeeAmount;
    }

    public Integer getGraceDays() {
        return graceDays;
    }

    public void setGraceDays(Integer graceDays) {
        this.graceDays = graceDays;
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
}
