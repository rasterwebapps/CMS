package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.SafetyGuidelineCategory;
import com.cms.model.enums.SafetyPriority;

public record SafetyGuidelineResponse(
    Long id,
    String title,
    String description,
    Long labId,
    String labName,
    Long departmentId,
    String departmentName,
    SafetyGuidelineCategory category,
    SafetyPriority priority,
    Boolean isActive,
    LocalDate effectiveDate,
    LocalDate reviewDate,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}

