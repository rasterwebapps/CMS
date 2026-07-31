package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FacultyAbsenceDto(
    Long id,
    Long facultyId,
    String facultyName,
    LocalDate absenceDate,
    String reason,
    String recordedBy,
    Instant createdAt,
    Instant updatedAt
) {}
