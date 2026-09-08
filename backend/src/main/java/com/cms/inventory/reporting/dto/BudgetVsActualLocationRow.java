package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;

public record BudgetVsActualLocationRow(
    Long locationId,
    String locationName,
    long budgetCount,
    BigDecimal allocatedAmount,
    BigDecimal consumedAmount,
    BigDecimal remainingAmount,
    boolean overAllocated
) {}
