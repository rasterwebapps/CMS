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
@Table(name = "semester_fees")
@EntityListeners(AuditingEntityListener.class)
public class SemesterFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private StudentFeeAllocation allocation;

    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @Column(name = "semester_label", nullable = false)
    private String semesterLabel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "semester_sequence", nullable = false)
    private Integer semesterSequence = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SemesterFee() {
    }

    public SemesterFee(StudentFeeAllocation allocation, Integer yearNumber,
                       String semesterLabel, BigDecimal amount, LocalDate dueDate) {
        this.allocation = allocation;
        this.yearNumber = yearNumber;
        this.semesterLabel = semesterLabel;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public SemesterFee(StudentFeeAllocation allocation, Integer yearNumber,
                       String semesterLabel, BigDecimal amount, LocalDate dueDate,
                       Integer semesterSequence) {
        this(allocation, yearNumber, semesterLabel, amount, dueDate);
        this.semesterSequence = semesterSequence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentFeeAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(StudentFeeAllocation allocation) {
        this.allocation = allocation;
    }

    public Integer getYearNumber() {
        return yearNumber;
    }

    public void setYearNumber(Integer yearNumber) {
        this.yearNumber = yearNumber;
    }

    public String getSemesterLabel() {
        return semesterLabel;
    }

    public void setSemesterLabel(String semesterLabel) {
        this.semesterLabel = semesterLabel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getSemesterSequence() {
        return semesterSequence;
    }

    public void setSemesterSequence(Integer semesterSequence) {
        this.semesterSequence = semesterSequence;
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
