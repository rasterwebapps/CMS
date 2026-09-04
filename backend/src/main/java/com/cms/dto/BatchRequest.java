package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BatchRequest(
    @NotNull(message = "Course offering ID is required")
    Long courseOfferingId,

    @NotBlank(message = "Batch name is required")
    String name,

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,

    Long coordinatorFacultyId,

    @NotNull(message = "Version is required")
    Long version
) {}
