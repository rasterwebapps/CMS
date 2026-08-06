package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** One Theory section to commit as part of a Cohort Room Allocation — {@code plannedSize} is
 *  admin-edited (auto-split by default, overridable), validated server-side against
 *  {@code classroomId}'s own capacity. */
public record CohortSectionRequest(
    @NotBlank String sectionLabel,
    @NotNull Long classroomId,
    @NotNull @Min(1) Integer plannedSize
) {}
