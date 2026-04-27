package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EnquiryYearWiseFeeStatusResponse(
    Long enquiryId,
    BigDecimal totalFee,
    BigDecimal totalPaid,
    BigDecimal totalOutstanding,
    List<YearFeeStatus> yearBreakdown,
    List<SemesterFeeStatus> semesterBreakdown
) {
    public record YearFeeStatus(
        int yearNumber,
        BigDecimal allocatedFee,
        BigDecimal paidAmount,
        BigDecimal outstanding
    ) {}

    public record SemesterFeeStatus(
        int semesterNumber,
        String semesterLabel,
        BigDecimal allocatedFee,
        BigDecimal paidAmount,
        BigDecimal outstanding,
        LocalDate dueDate
    ) {}
}
