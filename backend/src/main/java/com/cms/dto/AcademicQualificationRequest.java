package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.QualificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcademicQualificationRequest(
    @NotNull QualificationType qualificationType,
    @NotBlank String schoolName,
    String majorSubject,
    Integer totalMarks,
    BigDecimal percentage,
    String monthAndYearOfPassing,
    String universityOrBoard
) {}
