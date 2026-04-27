package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TermBillingScheduleRequest(
    @NotNull(message = "Academic year ID is required")
    Long academicYearId,

    @NotNull(message = "Term type is required")
    TermType termType,

    @NotNull(message = "Due date is required")
    LocalDate dueDate,

    @NotNull(message = "Late fee type is required")
    LateFeeType lateFeeType,

    @NotNull(message = "Late fee amount is required")
    @DecimalMin(value = "0.0", message = "Late fee amount must be non-negative")
    BigDecimal lateFeeAmount,

    @Min(value = 0, message = "Grace days must be non-negative")
    Integer graceDays
) {}
