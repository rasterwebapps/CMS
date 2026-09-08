package com.cms.inventory.budget.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BudgetResponse(
    Long id,
    Long locationId,
    String locationVirtualName,
    LocalDate periodStartDate,
    LocalDate periodEndDate,
    BigDecimal allocatedAmount,
    BigDecimal consumedAmount,
    BigDecimal remainingAmount,
    boolean overAllocated,
    String notes,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
