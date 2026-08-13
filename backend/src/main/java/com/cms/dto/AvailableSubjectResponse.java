package com.cms.dto;

import java.time.LocalTime;

public record AvailableSubjectResponse(
    Long classScheduleId,
    Long subjectId,
    String subjectName,
    String subjectCode,
    String batchName,
    String slotName,
    LocalTime startTime,
    LocalTime endTime
) {}
