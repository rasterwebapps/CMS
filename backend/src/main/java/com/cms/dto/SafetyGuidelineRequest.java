package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.SafetyGuidelineCategory;
import com.cms.model.enums.SafetyPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SafetyGuidelineRequest(
    @NotBlank String title,
    String description,
    Long labId,
    Long departmentId,
    @NotNull SafetyGuidelineCategory category,
    @NotNull SafetyPriority priority,
    @NotNull LocalDate effectiveDate,
    LocalDate reviewDate,
    String createdBy
) {}

