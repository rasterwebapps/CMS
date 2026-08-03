package com.cms.model.enums;

/** DAILY/WEEKLY were originally left out on the reasoning that a silent standing closure is
 *  already just a {@link BlockType#RECURRING} blocked_periods rule -- reconsidered so a repeating
 *  event created inline from the Add Event form (mirroring iOS/Google Calendar's Repeat picker)
 *  can be a real, named, calendar-visible event every day/week too, not only a silent block. */
public enum HolidayRecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
