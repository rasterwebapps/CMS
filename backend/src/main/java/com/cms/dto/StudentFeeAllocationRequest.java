package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StudentFeeAllocationRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Total fee is required")
    @PositiveOrZero(message = "Total fee must be zero or positive")
    BigDecimal totalFee,

    @PositiveOrZero(message = "Discount must be zero or positive")
    BigDecimal discountAmount,

    String discountReason,

    @PositiveOrZero(message = "Agent commission must be zero or positive")
    BigDecimal agentCommission,

    @NotEmpty(message = "Year-wise fee amounts are required")
    List<YearFee> yearFees
) {
    public record YearFee(
        @NotNull(message = "Year number is required")
        Integer yearNumber,

        @NotNull(message = "Amount is required")
        @PositiveOrZero(message = "Amount must be zero or positive")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        LocalDate dueDate
    ) {}
}
