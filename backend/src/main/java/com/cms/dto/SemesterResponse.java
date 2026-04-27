package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.SemesterStatus;

public record SemesterResponse(
    Long id,
    String name,
    AcademicYearResponse academicYear,
    LocalDate startDate,
    LocalDate endDate,
    Integer semesterNumber,
    SemesterStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
