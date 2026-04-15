package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.FeeAllocationStatus;

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
@Table(name = "student_fee_allocations")
@EntityListeners(AuditingEntityListener.class)
public class StudentFeeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "total_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discount_reason")
    private String discountReason;

    @Column(name = "agent_commission", precision = 12, scale = 2)
    private BigDecimal agentCommission;

    @Column(name = "net_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal netFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeAllocationStatus status;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "finalized_by")
    private String finalizedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StudentFeeAllocation() {
    }

    public StudentFeeAllocation(Student student, Program program, BigDecimal totalFee,
                                 BigDecimal discountAmount, String discountReason,
                                 BigDecimal agentCommission, BigDecimal netFee,
                                 FeeAllocationStatus status) {
        this.student = student;
        this.program = program;
        this.totalFee = totalFee;
        this.discountAmount = discountAmount;
        this.discountReason = discountReason;
        this.agentCommission = agentCommission;
        this.netFee = netFee;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public BigDecimal getAgentCommission() {
        return agentCommission;
    }

    public void setAgentCommission(BigDecimal agentCommission) {
        this.agentCommission = agentCommission;
    }

    public BigDecimal getNetFee() {
        return netFee;
    }

    public void setNetFee(BigDecimal netFee) {
        this.netFee = netFee;
    }

    public FeeAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(FeeAllocationStatus status) {
        this.status = status;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Instant finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(String finalizedBy) {
        this.finalizedBy = finalizedBy;
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
