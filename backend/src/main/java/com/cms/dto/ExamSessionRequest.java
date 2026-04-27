package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.ExamSessionType;

import jakarta.validation.constraints.NotNull;

public record ExamSessionRequest(
    @NotNull Long termInstanceId,
    @NotNull ExamSessionType sessionType,
    LocalDate startDate,
    LocalDate endDate
) {}
