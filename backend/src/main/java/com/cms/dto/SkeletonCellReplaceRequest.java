package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** Replace what a placed Theory cell teaches, keeping its day/period/audience exactly as they are.
 *
 *  <p>Subject and faculty are chosen together on purpose: a new subject's eligible teacher pool is
 *  usually different from the old one's, so replacing the subject alone would routinely leave the
 *  cell staffed by someone not eligible to teach it. Asking for both lets the two be validated as
 *  one atomic decision instead of leaving a window where the cell is internally inconsistent.
 *
 *  <p>No room field: a Theory room is never stored per session — it is derived from the section's
 *  committed Cohort Room Allocation ({@code cohortSection.getClassroom()}) every time the cell is
 *  staffed. Changing rooms is a Capacity Planner action that applies to the whole section, not
 *  something a single session can override. */
public record SkeletonCellReplaceRequest(
    @NotNull Long courseOfferingId,
    @NotNull Long facultyId
) {}
