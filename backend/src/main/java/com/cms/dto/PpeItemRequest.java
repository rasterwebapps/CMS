package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.PpeCategory;
import com.cms.model.enums.PpeCondition;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PpeItemRequest(
    @NotNull Long labId,
    @NotBlank String name,
    @NotNull PpeCategory category,
    @NotNull @Min(0) Integer totalQuantity,
    @NotNull @Min(0) Integer availableQuantity,
    @NotNull @Min(0) Integer minimumRequired,
    @NotNull PpeCondition condition,
    LocalDate lastInspectionDate,
    LocalDate nextInspectionDate
) {}

