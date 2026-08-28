package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** Atomically exchanges two already-placed DRAFT skeleton cells' day/period — the path invoked
 *  is {@code cells/{sourceCellId}/swap}, so this only needs to name the *other* side. */
public record SkeletonCellSwapRequest(
    @NotNull(message = "Target cell is required")
    Long targetCellId,

    /** Same rationale as {@link SkeletonCellPlacementRequest#cohortId()} — CourseOffering has no
     *  enforced Cohort FK, so the caller (which already has the cohort selected) passes it. */
    @NotNull(message = "Cohort is required")
    Long cohortId
) {}
