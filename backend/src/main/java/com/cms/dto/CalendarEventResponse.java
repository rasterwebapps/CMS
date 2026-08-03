package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    Instant updatedAt,
    /** Distinct period ids currently auto-blocked for this event (only ever non-empty for
     *  eventType == HOLIDAY). Empty for a manually-created block-less event, or before any sync
     *  has run. */
    List<Long> blockedPeriodIds,
    /** Non-null only when this event was seeded from a recurring HolidayTemplate -- drives the
     *  "delete this occurrence only" vs "delete this and all future occurrences" choice. */
    Long sourceHolidayTemplateId,
    String sourceHolidayTemplateName
) {
    public CalendarEventResponse(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
                                  CalendarEventType eventType, AcademicYearResponse academicYear,
                                  Instant createdAt, Instant updatedAt) {
        this(id, title, description, startDate, endDate, eventType, null, academicYear, createdAt, updatedAt,
            List.of(), null, null);
    }

    public CalendarEventResponse(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
                                  CalendarEventType eventType, HolidayCategory holidayCategory,
                                  AcademicYearResponse academicYear, Instant createdAt, Instant updatedAt) {
        this(id, title, description, startDate, endDate, eventType, holidayCategory, academicYear, createdAt,
            updatedAt, List.of(), null, null);
    }
}
