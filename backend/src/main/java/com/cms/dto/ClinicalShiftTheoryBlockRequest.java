package com.cms.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record ClinicalShiftTheoryBlockRequest(
    @NotNull(message = "Sequence order is required")
    Integer sequenceOrder,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @NotNull(message = "Subject ID is required")
    Long subjectId,

    Long classroomId
) {}
