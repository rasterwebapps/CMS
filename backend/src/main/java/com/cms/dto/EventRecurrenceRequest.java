package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

/** The "Repeats" configuration chosen inline on the Add/Edit Event form (mirrors a simplified
 *  iOS/Google Calendar Repeat picker) -- only read by {@code CalendarEventService} when the
 *  request's {@code repeats} flag is true. Anchored implicitly to the event's own startDate; the
 *  event's own title/description/holidayCategory/date-span become the generated
 *  {@code HolidayTemplate}'s corresponding fields, so this record only needs the parts that are
 *  genuinely new: frequency, interval, pattern, and an optional end date. */
public record EventRecurrenceRequest(
    HolidayRecurrenceType recurrenceType,

    /** "Every N [recurrenceType units]". Defaults to 1 if null. */
    Integer intervalCount,

    /** Null means repeats forever. */
    LocalDate endDate,

    /** YEARLY: required. */
    Integer month,

    /** YEARLY: required. MONTHLY fixed-day-of-month pattern: required (mutually exclusive with
     *  weekOfMonth/dayOfWeek). */
    Integer dayOfMonth,

    /** MONTHLY nth-weekday pattern only, paired with dayOfWeek. */
    WeekOfMonth weekOfMonth,

    /** Required for WEEKLY, and for MONTHLY's nth-weekday pattern (paired with weekOfMonth). */
    DayOfWeek dayOfWeek
) {}
