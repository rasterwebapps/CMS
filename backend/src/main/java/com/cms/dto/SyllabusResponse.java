package com.cms.dto;

import java.time.Instant;

public record SyllabusResponse(
    Long id,
    Long courseId,
    String courseName,
    String courseCode,
    Integer version,
    Integer theoryHours,
    Integer labHours,
    Integer tutorialHours,
    String objectives,
    String content,
    String textBooks,
    String referenceBooks,
    String courseOutcomes,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
