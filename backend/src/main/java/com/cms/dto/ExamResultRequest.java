package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.ExamResultStatus;

import jakarta.validation.constraints.NotNull;

public record ExamResultRequest(
    @NotNull Long examinationId,
    @NotNull Long studentId,
    BigDecimal marksObtained,
    String grade,
    ExamResultStatus status
) {}
