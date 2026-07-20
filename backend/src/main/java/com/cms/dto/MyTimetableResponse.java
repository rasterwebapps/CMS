package com.cms.dto;

import java.util.List;

public record MyTimetableResponse(
    List<ClassScheduleResponse> sessions,
    /** 0=Monday .. 5=Saturday, only populated when weekStart was supplied — see
     *  PersonalTimetableService for why holiday-awareness is display-time only. */
    List<Integer> holidayDayIndexes
) {}
