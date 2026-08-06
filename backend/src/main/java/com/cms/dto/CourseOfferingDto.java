package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.SubjectType;

public record CourseOfferingDto(
    Long id,
    Long termInstanceId,
    String termInstanceLabel,
    Long curriculumVersionId,
    String curriculumVersionName,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long subjectSpecialityId,
    String subjectSpecialityName,
    Integer termNumber,
    Long facultyId,
    String sectionLabel,
    Boolean isActive,
    Long curriculumTermCourseId,
    Boolean isElective,
    SubjectType subjectType,
    Long electiveGroupId,
    String electiveGroupName,
    Integer labHours,
    Integer clinicalHours,
    Instant createdAt,
    Instant updatedAt
) {}
