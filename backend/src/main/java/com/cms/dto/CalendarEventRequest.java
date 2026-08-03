package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarEventRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotNull(message = "Event type is required")
    CalendarEventType eventType,

    @NotNull(message = "Academic year ID is required")
    Long academicYearId,

    /** Required by service-layer validation when eventType == HOLIDAY; ignored otherwise. */
    HolidayCategory holidayCategory,

    /** Only meaningful when eventType == HOLIDAY -- which periods to auto-block for every date in
     *  [startDate, endDate]. Null or empty means "whole day" (every active period). Ignored for
     *  every other eventType. */
    List<Long> blockedPeriodIds
) {
    public CalendarEventRequest(String title, String description, LocalDate startDate, LocalDate endDate,
                                 CalendarEventType eventType, Long academicYearId) {
        this(title, description, startDate, endDate, eventType, academicYearId, null, null);
    }

    public CalendarEventRequest(String title, String description, LocalDate startDate, LocalDate endDate,
                                 CalendarEventType eventType, Long academicYearId, HolidayCategory holidayCategory) {
        this(title, description, startDate, endDate, eventType, academicYearId, holidayCategory, null);
    }
}
