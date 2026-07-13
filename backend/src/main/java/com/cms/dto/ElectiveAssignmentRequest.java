package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record ElectiveAssignmentRequest(
    @NotNull(message = "Enrollment ID is required")
    Long enrollmentId,

    @NotNull(message = "Course offering ID is required")
    Long courseOfferingId
) {}
