package com.cms.dto;

import java.time.Instant;
import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

public record LabScheduleResponse(
    Long id,
    Long labId,
    String labName,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long facultyId,
    String facultyName,
    Long labSlotId,
    String labSlotName,
    LocalTime startTime,
    LocalTime endTime,
    String batchName,
    DayOfWeek dayOfWeek,
    Long semesterId,
    String semesterName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
