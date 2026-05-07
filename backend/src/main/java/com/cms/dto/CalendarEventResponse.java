package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.CalendarEventType;

public record CalendarEventResponse(
    Long id,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    CalendarEventType eventType,
    AcademicYearResponse academicYear,
    Instant createdAt,
    Instant updatedAt
) {}
