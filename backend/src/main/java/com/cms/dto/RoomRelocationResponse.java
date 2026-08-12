package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.OccurrenceStatus;

public record RoomRelocationResponse(
    Long classScheduleId,
    LocalDate date,
    String venueName,
    OccurrenceStatus occurrenceStatus
) {}
