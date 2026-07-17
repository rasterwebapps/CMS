package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.ExamOutcome;
import com.cms.model.enums.ExamResultStatus;

public record ExamResultResponse(
    Long id,
    Long examinationId,
    String examinationName,
    Long studentId,
    String studentName,
    String studentRollNumber,
    BigDecimal marksObtained,
    String grade,
    ExamResultStatus status,
    ExamOutcome outcome,
    Instant createdAt,
    Instant updatedAt
) {}
