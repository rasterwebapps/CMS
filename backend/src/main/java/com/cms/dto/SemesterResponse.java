package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SemesterResponse(
    Long id,
    String name,
    AcademicYearResponse academicYear,
    LocalDate startDate,
    LocalDate endDate,
    Integer semesterNumber,
    Instant createdAt,
    Instant updatedAt
) {}
