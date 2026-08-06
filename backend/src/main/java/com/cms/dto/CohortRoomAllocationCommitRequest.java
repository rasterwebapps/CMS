package com.cms.dto;

import java.util.List;

import com.cms.model.enums.PlanningBasis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** {@code sections} must have at least one row -- every commit produces at least one Theory
 *  section, even the common unsectioned case. {@code ventureSplits} may be empty -- a cohort's
 *  term can in principle be pure Theory with no Lab/Clinical hours, so an empty list just means
 *  "no lab/clinical batches to create this commit." {@code planningBasis} records which strength
 *  number (live enrolled headcount vs. university-sanctioned intake) this commit was actually
 *  planned against. */
public record CohortRoomAllocationCommitRequest(
    @NotNull Long cohortId,
    @NotNull Long termInstanceId,
    @NotNull PlanningBasis planningBasis,
    @NotEmpty @Valid List<CohortSectionRequest> sections,
    @NotNull @Valid List<VentureSplitRequest> ventureSplits
) {}
