package com.cms.dto;

import java.time.LocalDate;

/** One row of the Faculty Absence list (GET /faculty-absences/page). affectedSessionCount and
 *  substitutedCount are resolved per row via FacultyAbsenceService#findAffectedSessions -- the
 *  same on-demand candidate resolution the detail screen uses -- since coverage isn't a stored
 *  column (see FacultyAbsenceService for why: it depends on ClassSchedule day-of-week + term
 *  dates + DayMappingOverride, not something a plain FacultyAbsence row can answer alone). */
public record FacultyAbsenceListItemResponse(
    Long id,
    Long facultyId,
    String facultyName,
    String specialityName,
    LocalDate absenceDate,
    String reason,
    String recordedBy,
    int affectedSessionCount,
    int substitutedCount
) {}
