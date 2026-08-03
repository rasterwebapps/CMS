package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayCategory;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HolidayTemplateRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Recurrence type is required")
    HolidayRecurrenceType recurrenceType,

    /** Which event type this template seeds. Defaults to HOLIDAY if null (matches the field's
     *  own default and every template created before this field existed). */
    CalendarEventType eventType,

    /** Only meaningful when eventType == HOLIDAY; ignored otherwise. */
    HolidayCategory holidayCategory,

    String description,

    /** Defaults to 1 (single-day) if null. */
    Integer durationDays,

    /** "Every N [recurrenceType units]". Defaults to 1 if null. */
    Integer intervalCount,

    /** Required whenever intervalCount > 1, and always for DAILY. Optional otherwise. */
    LocalDate anchorDate,

    /** Null means repeats forever. */
    LocalDate endDate,

    /** YEARLY: required. MONTHLY fixed-day-of-month pattern: null. */
    Integer month,

    /** YEARLY: required. MONTHLY fixed-day-of-month pattern: required (mutually exclusive with
     *  weekOfMonth/dayOfWeek). */
    Integer dayOfMonth,

    /** MONTHLY nth-weekday pattern only, paired with dayOfWeek. */
    WeekOfMonth weekOfMonth,

    /** Required for WEEKLY, and for MONTHLY's nth-weekday pattern (paired with weekOfMonth). */
    DayOfWeek dayOfWeek,

    Boolean isActive
) {}
