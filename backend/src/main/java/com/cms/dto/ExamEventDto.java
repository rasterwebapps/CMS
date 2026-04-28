package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;

public record ExamEventDto(
    Long id,
    Long examSessionId,
    ExamSessionType sessionType,
    ExamSessionStatus sessionStatus,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    LocalDate examDate,
    BigDecimal maxMarks,
    BigDecimal passMarks
) {}
