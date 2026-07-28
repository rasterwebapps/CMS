package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayCategory;

public record CalendarEventResponse(
    Long id,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    CalendarEventType eventType,
    HolidayCategory holidayCategory,
    AcademicYearResponse academicYear,
    Instant createdAt,
    Instant updatedAt
) {
    public CalendarEventResponse(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
                                  CalendarEventType eventType, AcademicYearResponse academicYear,
                                  Instant createdAt, Instant updatedAt) {
        this(id, title, description, startDate, endDate, eventType, null, academicYear, createdAt, updatedAt);
    }
}
