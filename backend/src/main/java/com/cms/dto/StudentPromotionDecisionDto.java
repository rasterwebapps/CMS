package com.cms.dto;

import java.time.Instant;
import java.util.List;

import com.cms.model.enums.PromotionOutcome;

public record StudentPromotionDecisionDto(
    Long id,
    Long studentId,
    String studentName,
    String rollNumber,
    Long cohortId,
    String cohortCode,
    Long fromTermInstanceId,
    String fromTermLabel,
    Long toTermInstanceId,
    String toTermLabel,
    PromotionOutcome outcome,
    List<PromotionArrearSubject> arrearSubjects,
    String decidedBy,
    Instant decidedAt,
    String remarks
) {}
