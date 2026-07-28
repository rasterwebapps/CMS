package com.cms.dto;

import java.util.List;

public record MyTimetableResponse(
    List<ClassScheduleResponse> sessions,
    /** Only populated when weekStart was supplied — see PersonalTimetableService for why
     *  holiday-awareness is display-time only. */
    List<HolidayDayInfo> holidays
) {}
