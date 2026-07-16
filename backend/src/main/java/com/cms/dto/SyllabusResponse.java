package com.cms.dto;

import java.time.Instant;

public record SyllabusResponse(
    Long id,
    Long curriculumTermCourseId,
    Long curriculumVersionId,
    String curriculumVersionName,
    Integer termNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Integer version,
    // Theory/Lab/Clinical hours are read-only here — Curriculum Map is the source of truth,
    // these are only ever derived from the linked curriculum_term_courses row.
    Integer theoryHours,
    Integer labHours,
    Integer clinicalHours,
    String objectives,
    String content,
    String textBooks,
    String referenceBooks,
    String courseOutcomes,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
