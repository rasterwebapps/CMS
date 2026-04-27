package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.RegistrationStatus;

public record CourseRegistrationDto(
    Long id,
    Long enrollmentId,
    Long studentId,
    String studentName,
    String cohortCode,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Integer semesterNumber,
    RegistrationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
