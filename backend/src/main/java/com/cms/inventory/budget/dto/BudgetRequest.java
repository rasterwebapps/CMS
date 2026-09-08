package com.cms.inventory.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BudgetRequest(
    @NotNull Long locationId,
    @NotNull LocalDate periodStartDate,
    @NotNull LocalDate periodEndDate,
    @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal allocatedAmount,
    @Size(max = 500) String notes,
    Boolean isActive
) {}
