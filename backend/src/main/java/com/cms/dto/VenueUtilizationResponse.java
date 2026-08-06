package com.cms.dto;

/** {@code claimedByCohortLabel} is non-null only for Classrooms with a genuine active
 *  CohortSection claim on them THIS term instance (from some other cohort, since this plan's own
 *  cohort would be shown via currentAllocation instead) -- Labs/Clinical Venues are never
 *  exclusively claimed per-term, so it's always null for those. */
public record VenueUtilizationResponse(
    Long id,
    String name,
    Integer capacity,
    long occupiedSlots,
    int totalSlots,
    double utilizationPercent,
    String claimedByCohortLabel
) {}
