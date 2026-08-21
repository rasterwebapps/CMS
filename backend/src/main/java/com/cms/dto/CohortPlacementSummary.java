package com.cms.dto;

public record CohortPlacementSummary(
    Long cohortId,
    String cohortName,
    int placedCount,
    int staffedCount
) {}
