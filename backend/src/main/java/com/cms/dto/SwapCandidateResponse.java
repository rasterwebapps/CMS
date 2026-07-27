package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

public record SwapCandidateResponse(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Long periodId,
    Long labSlotId,
    boolean occupied,
    Long occupyingSessionId,
    String occupyingSubjectName
) {}
