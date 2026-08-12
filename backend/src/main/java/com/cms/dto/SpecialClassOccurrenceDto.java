package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.SpecialClassApprovalStatus;

/** BR-55: display shape for a special-class/day-repeat {@code SessionOccurrence} row. Deliberately
 *  separate from {@link SessionOccurrenceDto} (the regular-session/progress-tracking DTO) rather
 *  than extending it — the two have almost no overlapping consumers, and keeping them apart avoids
 *  touching every existing {@code SessionOccurrenceDto} construction call site for a field set
 *  they'd never populate. */
public record SpecialClassOccurrenceDto(
    Long id,
    OccurrenceSource occurrenceSource,
    LocalDate occurrenceDate,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long courseOfferingId,
    Long cohortSectionId,
    String cohortSectionLabel,
    Long periodId,
    String periodName,
    LocalTime periodStartTime,
    LocalTime periodEndTime,
    ClassSessionType sessionType,
    Long venueId,
    String venueName,
    Long requestedFacultyId,
    String requestedFacultyName,
    SpecialClassApprovalStatus approvalStatus,
    Long requestedByFacultyId,
    String requestedByFacultyName,
    Instant requestedAt,
    String requestReason,
    DayOfWeek sourceDayOfWeek,
    UUID requestBatchId,
    String approvedBy,
    Instant approvedAt,
    String rejectionReason
) {}
