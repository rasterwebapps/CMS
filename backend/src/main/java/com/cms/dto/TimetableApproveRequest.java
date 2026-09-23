package com.cms.dto;

import java.util.List;

/** Body for {@code POST /timetables/{termInstanceId}/approve}. {@code cohortIds} is required (OC-260
 *  made Approve cohort-scoped -- there is no "empty means whole term" shortcut). {@code
 *  overrideIncompleteCoverage=false} is the default first attempt; the frontend only ever sends
 *  {@code overrideIncompleteCoverage=true} as a resubmission after {@link TimetableCoverageGapException}
 *  was shown to the user and an authorized reviewer (holding {@code TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE})
 *  typed a reason — see {@code TimetableController#approve}'s {@code @PreAuthorize}, which is the actual
 *  enforcement point. */
public record TimetableApproveRequest(
    List<Long> cohortIds,
    boolean overrideIncompleteCoverage,
    String overrideReason
) {}
