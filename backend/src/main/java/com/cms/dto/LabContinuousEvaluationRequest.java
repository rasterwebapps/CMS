package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record LabContinuousEvaluationRequest(
    @NotNull Long experimentId,
    @NotNull Long studentId,
    Integer recordMarks,
    Integer vivaMarks,
    Integer performanceMarks,
    Integer totalMarks,
    LocalDate evaluationDate,
    String evaluatedBy
) {}
