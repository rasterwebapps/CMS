package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.ClassSessionType;

import jakarta.validation.constraints.NotNull;

/** BR-55: a single-subject special/remedial class request. Exactly one of {@code classroomId}/
 *  {@code labId}/{@code clinicalVenueId} must be supplied, matching {@code sessionType} — mirrors
 *  how a {@code ClassSchedule} row's own three-way venue split works. */
public record SpecialClassRequest(
    @NotNull(message = "Date is required") LocalDate occurrenceDate,
    @NotNull(message = "Period is required") Long periodId,
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
