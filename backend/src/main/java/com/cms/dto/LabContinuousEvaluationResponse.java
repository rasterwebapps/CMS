package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record LabContinuousEvaluationResponse(
    Long id,
    Long experimentId,
    String experimentName,
    Long studentId,
    String studentName,
    String studentRollNumber,
    Integer recordMarks,
    Integer vivaMarks,
    Integer performanceMarks,
    Integer totalMarks,
    LocalDate evaluationDate,
    String evaluatedBy,
    Instant createdAt,
    Instant updatedAt
) {}
