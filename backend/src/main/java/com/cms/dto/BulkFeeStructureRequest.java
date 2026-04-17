package com.cms.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkFeeStructureRequest(
    @NotNull(message = "Program ID is required")
    Long programId,

    @NotNull(message = "Academic Year ID is required")
    Long academicYearId,

    Long courseId,

    @NotNull(message = "Fee items are required")
    @Size(min = 1, message = "At least one fee item is required")
    List<@Valid FeeStructureItemRequest> items
) {}
