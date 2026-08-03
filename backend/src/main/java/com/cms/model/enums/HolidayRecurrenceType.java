package com.cms.model.enums;

/** WEEKLY is deliberately not a value here -- a standing weekly closure (e.g. every Sunday) is
 *  already just a {@link BlockType#RECURRING} blocked_periods rule with no CalendarEvent needed;
 *  duplicating that in HolidayTemplate would be a second mechanism for the same thing. */
public enum HolidayRecurrenceType {
    YEARLY,
    MONTHLY
}
