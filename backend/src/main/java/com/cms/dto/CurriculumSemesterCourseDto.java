package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.SubjectType;

public record CurriculumSemesterCourseDto(
    Long id,
    Long curriculumVersionId,
    String curriculumVersionName,
    Integer termNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Integer sortOrder,
    Integer theoryHours,
    Integer labHours,
    Integer clinicalHours,
    SubjectType subjectType,
    Boolean isElective,
    Long electiveGroupId,
    String electiveGroupName,
    Long courseId,
    String courseName,
    Instant createdAt,
    Instant updatedAt
) {}
