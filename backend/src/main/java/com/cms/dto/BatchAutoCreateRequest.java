package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchAutoCreateRequest(
    @NotNull(message = "Course offering ID is required")
    Long courseOfferingId,

    @NotNull(message = "Batch count is required")
    @Min(value = 1, message = "At least 1 batch is required")
    Integer count,

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity
) {}
