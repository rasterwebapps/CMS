package com.cms.dto;

import java.math.BigDecimal;

public record FeeCollectionSummaryDto(
    String programName,
    String programCode,
    long totalDemands,
    BigDecimal totalAmount,
    BigDecimal collectedAmount,
    BigDecimal outstandingAmount,
    long paidCount,
    long partialCount,
    long unpaidCount
) {}
