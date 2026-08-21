package com.cms.dto;

import java.util.List;

import com.cms.model.enums.ClassSessionType;

/** One auto-suggested Lab/Clinical batch row for a {@link SuggestedSectionResponse} — sessionType
 *  is always LAB or CLINICAL. batchLabel is null when the section's whole headcount fits the
 *  suggested venue unsplit, "Batch 1"/"Batch 2"... when the venue was too small and the row was
 *  greedily packed into multiple venue-sized batches. eligibleVenueIds is the subject's own
 *  configured-eligible venue IDs for this session type (Subject.eligibleLabs/
 *  eligibleClinicalVenues, active ones only) — always present even when {@code venueId} itself came
 *  from the full-pool fallback, so manual pickers can still sort/highlight the subject's real
 *  preference without a second lookup. Empty when the subject has no eligible venues configured. */
public record SuggestedBatchResponse(
    Long courseOfferingId,
    String subjectName,
    ClassSessionType sessionType,
    Long venueId,
    String venueName,
    Integer venueCapacity,
    String sectionLabel,
    String batchLabel,
    int plannedSize,
    List<Long> eligibleVenueIds
) {}
