package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeeExplorerResponse(
    List<StudentFeeSummary> students
) {
    public record StudentFeeSummary(
        Long studentId,
        String studentName,
        String rollNumber,
        String programName,
        Integer durationYears,
        BigDecimal totalFee,
        BigDecimal totalPaid,
        BigDecimal totalPending,
        BigDecimal totalPenalty,
        String allocationStatus
    ) {}
}
