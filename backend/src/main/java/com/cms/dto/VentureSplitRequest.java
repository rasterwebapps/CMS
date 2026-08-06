package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** One Lab/Clinical batch to create as part of a Cohort Room Allocation commit — {@code venueId}
 *  resolves to a Lab id when {@code sessionType} is LAB, or a ClinicalVenue id when CLINICAL.
 *  {@code plannedSize} is admin-edited, not forced to an even split (e.g. 30/30 instead of 40/20
 *  for a 60-strong cohort splitting across two labs). {@code cohortSectionLabel} is optional: when
 *  the commit has exactly one section it auto-resolves to that section, but must be supplied and
 *  is validated when the commit has 2+ sections (which section's sub-cohort this batch draws from
 *  can't be guessed). It correlates to {@code sections[].sectionLabel} in the same commit request
 *  by label, not a real {@code CohortSection} id — the sections being created in this same commit
 *  don't have database ids yet when the request is built. */
public record VentureSplitRequest(
    @NotNull Long courseOfferingId,
    @NotNull ClassSessionType sessionType,
    @NotNull Long venueId,
    @NotBlank String batchName,
    @NotNull @Min(1) Integer plannedSize,
    String cohortSectionLabel
) {}
