package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExamEventRequest(
    @NotNull Long examSessionId,
    @NotNull Long courseOfferingId,
    LocalDate examDate,
    @NotNull @Positive BigDecimal maxMarks,
    @NotNull @Positive BigDecimal passMarks
) {}
