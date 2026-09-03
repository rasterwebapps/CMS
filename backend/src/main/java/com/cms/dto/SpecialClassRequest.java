package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.ClassSessionType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** BR-55: a single-subject special/remedial class request. Exactly one of {@code classroomId}/
 *  {@code labId}/{@code clinicalVenueId} must be supplied, matching {@code sessionType} — mirrors
 *  how a {@code ClassSchedule} row's own three-way venue split works. {@code periodIds} must name
 *  a single consecutive block (one period, or several back-to-back with no gap) — see {@code
 *  SpecialClassRequestService#resolveConsecutivePeriods}; each period becomes its own {@code
 *  SessionOccurrence} row, all sharing one {@code requestBatchId} so they're approved/rejected as
 *  one atomic unit via the existing day-repeat batch endpoints. */
public record SpecialClassRequest(
    @NotNull(message = "Date is required") LocalDate occurrenceDate,
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
