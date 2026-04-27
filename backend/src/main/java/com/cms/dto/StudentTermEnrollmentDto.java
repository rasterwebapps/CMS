package com.cms.dto;

import com.cms.model.enums.EnrollmentStatus;

public record StudentTermEnrollmentDto(
    Long id,
    Long studentId,
    String studentName,
    Long cohortId,
    String cohortCode,
    Long termInstanceId,
    String termInstanceLabel,
    Integer semesterNumber,
    Integer yearOfStudy,
    EnrollmentStatus status
) {}
