package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CohortSeatAllocationRequest(
    @NotNull(message = "Course is required")
    Long courseId,

    @Min(value = 0, message = "Management seats must be 0 or more")
    Integer managementSeats,

    @Min(value = 0, message = "Counselling seats must be 0 or more")
    Integer counsellingSeats
) {}
