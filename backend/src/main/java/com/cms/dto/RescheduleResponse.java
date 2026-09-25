package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.OccurrenceStatus;

public record RescheduleResponse(
    Long classScheduleId,
    LocalDate date,
    LocalDate targetDate,
    String periodName,
    String venueName,
    OccurrenceStatus occurrenceStatus
) {}
