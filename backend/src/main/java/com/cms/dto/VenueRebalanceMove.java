package com.cms.dto;

/** One committed {@link com.cms.model.Batch} a Rebalance operation will move (in a preview) or has
 *  moved (in a result) off an over/tight-capacity venue onto a better-fitting one it's already
 *  eligible for. {@code sessionsToClearCount} is the real, already-placed DRAFT {@code
 *  ClassSchedule} rows riding on this batch that get deactivated so the next Run Automation can
 *  re-place it at {@code toVenueId} — see {@code TimetableCapacityPlanningService#previewRebalance}. */
public record VenueRebalanceMove(
    Long batchId,
    String batchName,
    String subjectName,
    String cohortName,
    String sectionLabel,
    Integer plannedSize,
    Long fromVenueId,
    String fromVenueName,
    Long toVenueId,
    String toVenueName,
    int sessionsToClearCount
) {}
