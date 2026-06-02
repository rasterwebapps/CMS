package com.cms.dto;

import java.util.List;

public record CohortLapsedSummaryResponse(
    List<CohortLapsedRow> cohorts,
    long   totalCounsellingSeats,
    long   totalFilledCounselling,
    long   totalLapsedSeats,
    double lapsedPercentage
) {
    public record CohortLapsedRow(
        Long   cohortId,
        String courseName,
        String courseCode,
        long   counsellingSeats,
        long   filledCounselling,
        long   lapsedSeats,
        boolean counsellingClosed
    ) {}
}
