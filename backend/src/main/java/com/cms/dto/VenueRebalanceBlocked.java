package com.cms.dto;

/** One batch a Rebalance preview identified as needing to move but couldn't find a home for —
 *  surfaced explicitly rather than silently dropped, since a batch left behind still leaves the
 *  source venue over its weekly window. */
public record VenueRebalanceBlocked(
    Long batchId,
    String batchName,
    String subjectName,
    String reason
) {}
