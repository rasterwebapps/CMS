package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** BR-55: copies every subject/period from a source weekday's recurring timetable onto a
 *  different target date, for one cohort section. Creates one {@link com.cms.model.SessionOccurrence}
 *  per resolvable source {@code ClassSchedule} row, all sharing one {@code requestBatchId} —
 *  each copied row's faculty is inherited directly from its source {@code ClassSchedule.faculty},
 *  not a single faculty for the whole day. Rows whose cohort ownership can't be unambiguously
 *  resolved (see {@code SpecialClassRequestService}) are skipped rather than guessed, and
 *  reported back via the skipped count. */
public record DayRepeatRequest(
    @NotNull(message = "Term is required") Long termInstanceId,
    @NotNull(message = "Source weekday is required") DayOfWeek sourceDayOfWeek,
    @NotNull(message = "Target date is required") LocalDate targetDate,
    @NotNull(message = "Cohort section is required") Long cohortSectionId,
    String reason
) {}
