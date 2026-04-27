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
    BigDecimal agentCommission,
    BigDecimal netFee,
    String status,
    Instant finalizedAt,
    String finalizedBy,
    List<SemesterFeeDetail> semesterFees,
    Instant createdAt,
    Instant updatedAt
) {
    public record SemesterFeeDetail(
        Long id,
        Integer yearNumber,
        Integer semesterSequence,
        String semesterLabel,
        BigDecimal amount,
        LocalDate dueDate,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        BigDecimal penaltyAmount,
        String paymentStatus
    ) {}
}
