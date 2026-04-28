package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;

public record ExamSessionDto(
    Long id,
    Long termInstanceId,
    String termInstanceLabel,
    ExamSessionType sessionType,
    ExamSessionStatus status,
    LocalDate startDate,
    LocalDate endDate
) {}
