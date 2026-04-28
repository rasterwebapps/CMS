package com.cms.dto;

import java.time.Instant;

public record CurriculumSemesterCourseDto(
    Long id,
    Long curriculumVersionId,
    String curriculumVersionName,
    Integer semesterNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Integer sortOrder,
    Instant createdAt,
    Instant updatedAt
) {}
