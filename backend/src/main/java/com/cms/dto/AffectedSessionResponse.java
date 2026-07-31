package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.OccurrenceStatus;

public record AffectedSessionResponse(
    Long classScheduleId,
    String subjectName,
    String subjectCode,
    String roomName,
    String slotName,
    LocalTime startTime,
    LocalTime endTime,
    String batchName,
    OccurrenceStatus occurrenceStatus,
    String substituteFacultyName
) {}
