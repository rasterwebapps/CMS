package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record ElectiveBulkAssignmentRequest(
    @NotNull(message = "Term instance ID is required")
    Long termInstanceId,

    @NotNull(message = "Elective group ID is required")
    Long electiveGroupId,

    @NotNull(message = "Course offering ID is required")
    Long courseOfferingId
) {}
