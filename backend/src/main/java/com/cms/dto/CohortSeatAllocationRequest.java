package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CohortSeatAllocationRequest(
    @NotNull(message = "Course is required")
    Long courseId,

    @Min(value = 0, message = "Total seats must be 0 or more")
    Integer totalSeats,

    @DecimalMin(value = "0.0", message = "Management percentage must be 0 or more")
    @DecimalMax(value = "100.0", message = "Management percentage cannot exceed 100")
    BigDecimal managementPercentage
) {}
