package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentFeeStatusResponse(
    Long studentId,
    String studentName,
    String rollNumber,
    BigDecimal totalFees,
    BigDecimal totalPaid,
    BigDecimal pendingAmount,
    List<FeeItemStatus> feeItems
) {
    public record FeeItemStatus(
        Long feeStructureId,
        String feeType,
        BigDecimal amount,
        BigDecimal paid,
        BigDecimal pending,
        String status
    ) {}
}
