package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.ClassSessionType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** BR-55: a weekly-recurring special/remedial class request — the same single-subject shape as
 *  {@link SpecialClassRequest}, repeated every week on {@code startDate}'s weekday through {@code
 *  endDate} inclusive. One {@link com.cms.model.SessionOccurrence} per eligible week, all sharing
 *  one {@code requestBatchId} so they're approved/rejected as one atomic unit via the existing
 *  batch endpoints — see {@code SpecialClassRequestService#requestRecurringSpecialClass}. */
public record RecurringSpecialClassRequest(
    @NotNull(message = "Start date is required") LocalDate startDate,
    @NotNull(message = "End date is required") LocalDate endDate,
    @NotEmpty(message = "At least one period is required") List<Long> periodIds,
    @NotNull(message = "Subject is required") Long subjectId,
    @NotNull(message = "Course offering is required") Long courseOfferingId,
    Long cohortSectionId,
    @NotNull(message = "Session type is required") ClassSessionType sessionType,
    Long classroomId,
    Long labId,
    Long clinicalVenueId,
    @NotNull(message = "Faculty is required") Long requestedFacultyId,
    String reason
) {}
