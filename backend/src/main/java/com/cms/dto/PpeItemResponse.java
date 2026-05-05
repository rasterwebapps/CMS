package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.PpeCategory;
import com.cms.model.enums.PpeCondition;

public record PpeItemResponse(
    Long id,
    Long labId,
    String labName,
    String name,
    PpeCategory category,
    Integer totalQuantity,
    Integer availableQuantity,
    Integer minimumRequired,
    PpeCondition condition,
    LocalDate lastInspectionDate,
    LocalDate nextInspectionDate,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}

