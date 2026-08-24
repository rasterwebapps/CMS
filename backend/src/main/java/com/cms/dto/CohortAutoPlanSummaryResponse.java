package com.cms.dto;

import java.util.List;

/** One row of the term-wide Capacity Auto-Plan overview — one per Cohort enrolled in a
 *  TermInstance. Committed cohorts are surfaced as-is (never re-planned); suggestedSections/
 *  suggestedLabClinicalBatches are always empty for a committed cohort — carrying the full lists
 *  (not just counts) lets the bulk screen render each cohort's suggested venue/capacity detail
 *  inline without a second per-cohort call. committedSectionsCount/committedBatchesCount are the
 *  mirror image -- always 0 for a not-yet-committed cohort, and the real active CohortSection/
 *  Batch row counts for a committed one -- so the term-wide stat tiles can report a genuine
 *  planned-vs-total ratio (e.g. "16/16") instead of the count silently dropping to 0 once
 *  everything commits (suggestedSections/suggestedLabClinicalBatches go empty at that point). */
public record CohortAutoPlanSummaryResponse(
    Long cohortId,
    String cohortLabel,
    Integer semesterNumber,
    long cohortStrength,
    boolean hasCommittedAllocation,
    boolean theoryFits,
    String theoryShortfallMessage,
    List<SuggestedSectionResponse> suggestedSections,
    List<SuggestedBatchResponse> suggestedLabClinicalBatches,
    boolean labClinicalMappingSufficient,
    String labClinicalMappingIssuesMessage,
    int committedSectionsCount,
    int committedBatchesCount
) {}
