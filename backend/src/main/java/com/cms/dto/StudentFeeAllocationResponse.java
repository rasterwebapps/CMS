package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudentFeeAllocationResponse(
    Long id,
    Long studentId,
    String studentName,
    String rollNumber,
    Long programId,
    String programName,
    BigDecimal totalFee,
    BigDecimal discountAmount,
    String discountReason,
    Long scholarshipApplicationId,
    BigDecimal scholarshipDiscountAmount,
    String scholarshipDiscountReason,
    BigDecimal agentCommission,
    BigDecimal netFee,
    String status,
    Instant finalizedAt,
    String finalizedBy,
    List<InstallmentFeeDetail> installmentFees,
    Instant createdAt,
    Instant updatedAt
) {
    public StudentFeeAllocationResponse(Long id, Long studentId, String studentName, String rollNumber,
                                        Long programId, String programName, BigDecimal totalFee,
                                        BigDecimal discountAmount, String discountReason,
                                        BigDecimal agentCommission, BigDecimal netFee, String status,
                                        Instant finalizedAt, String finalizedBy,
                                        List<InstallmentFeeDetail> installmentFees,
                                        Instant createdAt, Instant updatedAt) {
        this(id, studentId, studentName, rollNumber, programId, programName, totalFee, discountAmount,
            discountReason, null, null, null, agentCommission, netFee, status, finalizedAt, finalizedBy,
            installmentFees, createdAt, updatedAt);
    }

    public record InstallmentFeeDetail(
        Long id,
        Integer yearNumber,
        Integer sequence,
        String installmentLabel,
        BigDecimal amount,
        LocalDate dueDate,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        BigDecimal penaltyAmount,
        String paymentStatus
    ) {}
}
