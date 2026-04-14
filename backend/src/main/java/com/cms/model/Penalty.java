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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "penalties")
@EntityListeners(AuditingEntityListener.class)
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_fee_id", nullable = false)
    private SemesterFee semesterFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "penalty_start_date", nullable = false)
    private LocalDate penaltyStartDate;

    @Column(name = "penalty_end_date")
    private LocalDate penaltyEndDate;

    @Column(name = "total_penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPenalty;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Penalty() {
    }

    public Penalty(SemesterFee semesterFee, Student student, BigDecimal dailyRate,
                   LocalDate penaltyStartDate, BigDecimal totalPenalty) {
        this.semesterFee = semesterFee;
        this.student = student;
        this.dailyRate = dailyRate;
        this.penaltyStartDate = penaltyStartDate;
        this.totalPenalty = totalPenalty;
        this.isPaid = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SemesterFee getSemesterFee() {
        return semesterFee;
    }

    public void setSemesterFee(SemesterFee semesterFee) {
        this.semesterFee = semesterFee;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public LocalDate getPenaltyStartDate() {
        return penaltyStartDate;
    }

    public void setPenaltyStartDate(LocalDate penaltyStartDate) {
        this.penaltyStartDate = penaltyStartDate;
    }

    public LocalDate getPenaltyEndDate() {
        return penaltyEndDate;
    }

    public void setPenaltyEndDate(LocalDate penaltyEndDate) {
        this.penaltyEndDate = penaltyEndDate;
    }

    public BigDecimal getTotalPenalty() {
        return totalPenalty;
    }

    public void setTotalPenalty(BigDecimal totalPenalty) {
        this.totalPenalty = totalPenalty;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
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
