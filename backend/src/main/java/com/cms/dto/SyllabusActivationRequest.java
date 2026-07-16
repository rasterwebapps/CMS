package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** A syllabus version is immutable once created — this is the only permitted change to an
 *  existing row. Activating a version deactivates every other version for the same
 *  curriculum mapping (mirrors AcademicYearRepository.clearCurrentAcademicYear()'s pattern). */
public record SyllabusActivationRequest(
    @NotNull(message = "isActive is required")
    Boolean isActive
) {}
