package com.cms.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** {@code ventureSplits} may be empty — a cohort's term can in principle be pure Theory with no
 *  Lab/Clinical hours, so an empty list just means "no lab/clinical batches to create this commit." */
public record CohortRoomAllocationCommitRequest(
    @NotNull Long cohortId,
    @NotNull Long termInstanceId,
    @NotNull Long theoryClassroomId,
    @NotNull @Valid List<VentureSplitRequest> ventureSplits
) {}
