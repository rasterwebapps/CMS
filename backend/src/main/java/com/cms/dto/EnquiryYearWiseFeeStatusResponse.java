package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record EnquiryYearWiseFeeStatusResponse(
    Long enquiryId,
    BigDecimal totalFee,
    BigDecimal totalPaid,
    BigDecimal totalOutstanding,
    List<YearFeeStatus> yearBreakdown
) {
    public record YearFeeStatus(
        int yearNumber,
        BigDecimal allocatedFee,
        BigDecimal paidAmount,
        BigDecimal outstanding
    ) {}
}
