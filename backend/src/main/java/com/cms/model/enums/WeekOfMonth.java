package com.cms.model.enums;

/** Ordinal used by {@code HolidayTemplate} MONTHLY recurrence (e.g. "2nd Saturday of every
 *  month"). Maps to {@code java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(n, dow)} for
 *  FIRST..FOURTH, and {@code TemporalAdjusters.lastInMonth(dow)} for LAST. */
public enum WeekOfMonth {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    LAST
}
