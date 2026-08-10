package com.cms.dto;

import java.util.List;

/** R3.1 cohort-wide shape — one response now covers every non-elective subject a cohort has in a
 *  term, so cross-subject placement conflicts are visible in a single grid instead of hidden
 *  behind a per-subject filter. {@code subjects} carries each subject's own budget rows;
 *  {@code cells} and {@code batches} are merged across all of them, each still tagged with its
 *  owning {@code courseOfferingId}. */
public record SkeletonBuilderResponse(
    Long cohortId,
    String cohortName,
    String termInstanceLabel,
    List<SkeletonSubjectResponse> subjects,
    List<SkeletonCellResponse> cells,
    List<BatchDto> batches
) {}
