package com.cms.dto;

import com.cms.model.enums.HolidayCategory;

/** 0=Monday .. 5=Saturday, only populated when weekStart was supplied to /my-timetable — see
 *  PersonalTimetableService for why holiday-awareness is display-time only. */
public record HolidayDayInfo(
    int dayIndex,
    String title,
    HolidayCategory category
) {}
