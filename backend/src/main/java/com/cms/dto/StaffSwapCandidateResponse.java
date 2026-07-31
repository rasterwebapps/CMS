package com.cms.dto;

import java.time.LocalTime;

public record StaffSwapCandidateResponse(
    Long classScheduleId,
    String subjectName,
    String facultyName,
    LocalTime startTime,
    LocalTime endTime
) {}
