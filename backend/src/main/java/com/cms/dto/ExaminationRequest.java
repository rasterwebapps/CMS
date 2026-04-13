package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.ExamType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExaminationRequest(
    @NotBlank String name,
    @NotNull Long courseId,
    @NotNull ExamType examType,
    LocalDate date,
    Integer duration,
    Integer maxMarks,
    Long semesterId
) {}
