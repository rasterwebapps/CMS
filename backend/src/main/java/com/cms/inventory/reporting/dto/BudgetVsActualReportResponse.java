package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BudgetVsActualReportResponse(
    List<BudgetVsActualLocationRow> locations,
    long grandTotalBudgetCount,
    BigDecimal grandTotalAllocatedAmount,
    BigDecimal grandTotalConsumedAmount,
    BigDecimal grandTotalRemainingAmount,
    Instant generatedAt
) {}
