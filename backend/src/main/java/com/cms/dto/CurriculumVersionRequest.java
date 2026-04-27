package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CurriculumVersionRequest(
    @NotNull(message = "Program ID is required")
    Long programId,

    @NotBlank(message = "Version name is required")
    @Size(max = 100, message = "Version name must not exceed 100 characters")
    String versionName,

    @NotNull(message = "Effective from academic year ID is required")
    Long effectiveFromAcademicYearId,

    Boolean isActive
) {}
