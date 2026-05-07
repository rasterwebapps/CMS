package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record ScholarshipApplicationRequest(
    @NotNull(message = "Scholarship type ID is required")
    Long scholarshipTypeId,

    Long academicYearId,
    String applicationRemarks
) {}

