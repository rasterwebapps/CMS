package com.cms.dto;

import java.util.List;

/** Logs syllabus-unit coverage against an already-identified {@link com.cms.model.SessionOccurrence}
 *  row directly by its own id -- for a SPECIAL_CLASS/DAY_REPEAT/RECURRING_SPECIAL_CLASS occurrence,
 *  which (unlike a REGULAR one) has no ClassSchedule and exactly one fixed date already set at
 *  request time, so there's no classScheduleId/occurrenceDate pair to resolve it from. */
public record OccurrenceCoverageRequest(
    List<UnitCoverageRequest> units,
    String remarks
) {}
