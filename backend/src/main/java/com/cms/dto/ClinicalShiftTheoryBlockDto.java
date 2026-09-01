package com.cms.dto;

import java.time.LocalTime;

public record ClinicalShiftTheoryBlockDto(
    Long id,
    Integer sequenceOrder,
    LocalTime startTime,
    LocalTime endTime,
    Long subjectId,
    String subjectName,
    Long classroomId,
    String classroomName
) {}
