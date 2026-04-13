package com.cms.dto;

import java.time.Instant;

public record ExperimentResponse(
    Long id,
    Long courseId,
    String courseName,
    String courseCode,
    Integer experimentNumber,
    String name,
    String description,
    String aim,
    String apparatus,
    String procedure,
    String expectedOutcome,
    String learningOutcomes,
    Integer estimatedDurationMinutes,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
