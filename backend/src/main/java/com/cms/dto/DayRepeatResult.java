package com.cms.dto;

import java.util.List;

/** BR-55: result of a whole-day-repeat submission — {@code skippedCount} is the number of source
 *  rows whose cohort ownership couldn't be unambiguously resolved (see
 *  {@code SpecialClassRequestService}) and were therefore left out rather than guessed. */
public record DayRepeatResult(
    List<SpecialClassOccurrenceDto> created,
    int skippedCount
) {}
