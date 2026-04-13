package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.QualificationType;

public record AcademicQualificationResponse(
    Long id,
    Long admissionId,
    QualificationType qualificationType,
    String schoolName,
    String majorSubject,
    Integer totalMarks,
    BigDecimal percentage,
    String monthAndYearOfPassing,
    String universityOrBoard,
    Instant createdAt,
    Instant updatedAt
) {}
