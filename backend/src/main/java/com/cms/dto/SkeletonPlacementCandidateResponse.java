package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

/** One free (day, period) slot suggested for a subject/session-type/batch still short of its
 *  weekly budget — read-only, doesn't reserve anything; the frontend still calls the normal
 *  cell-placement endpoint once the admin picks one. */
public record SkeletonPlacementCandidateResponse(
    DayOfWeek dayOfWeek,
    Long periodId
) {}
