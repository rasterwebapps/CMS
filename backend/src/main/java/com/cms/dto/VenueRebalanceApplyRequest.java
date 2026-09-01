package com.cms.dto;

import java.util.List;

import com.cms.model.enums.ClassSessionType;

/** {@code batchIds} must be exactly the {@code batchId}s from a {@code VenueRebalancePreview}'s
 *  {@code willMove} list the admin actually confirmed — see {@code
 *  TimetableCapacityPlanningService#applyRebalance} for why every one is still re-validated
 *  server-side rather than trusted as-is. */
public record VenueRebalanceApplyRequest(
    ClassSessionType sessionType,
    Long venueId,
    List<Long> batchIds
) {}
