package com.cms.dto;

import java.util.List;

/** BR-55: result of a weekly-recurring special-class submission — {@code skippedCount} is the
 *  number of candidate weekly dates that were not themselves a non-instruction day (see {@code
 *  SpecialClassRequestService#requireNonInstructionDay}) and were therefore left out rather than
 *  requested, mirroring {@link DayRepeatResult}'s skip-and-report convention. */
public record RecurringSpecialClassResult(
    List<SpecialClassOccurrenceDto> created,
    int skippedCount
) {}
