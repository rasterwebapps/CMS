package com.cms.dto;

import java.util.List;

import com.cms.model.enums.ClassSessionType;

/** Preview for "Rebalance now" — the minimum set of already-committed batches that need to move
 *  off {@code venueId} to bring it back under its own weekly window, each paired with the
 *  best-fitting alternate venue its subject is already eligible for. Nothing is applied until the
 *  admin confirms via {@code TimetableCapacityPlanningService#applyRebalance} with the exact same
 *  batch ids echoed back — see that method's javadoc for why every batch is re-validated at apply
 *  time rather than trusted from this preview. */
public record VenueRebalancePreview(
    Long venueId,
    String venueName,
    ClassSessionType sessionType,
    int weeklyAvailablePeriods,
    int currentWeeklyDemandPeriods,
    List<VenueRebalanceMove> willMove,
    List<VenueRebalanceBlocked> notMovable
) {}
