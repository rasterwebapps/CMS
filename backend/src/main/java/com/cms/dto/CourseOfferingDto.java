package com.cms.dto;

import java.time.Instant;

public record CourseOfferingDto(
    Long id,
    Long termInstanceId,
    String termInstanceLabel,
    Long curriculumVersionId,
    String curriculumVersionName,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Integer semesterNumber,
    Long facultyId,
    String sectionLabel,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
