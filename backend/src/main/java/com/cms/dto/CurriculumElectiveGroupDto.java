package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.ElectiveSelectionMode;

public record CurriculumElectiveGroupDto(
    Long id,
    Long curriculumVersionId,
    Integer termNumber,
    String groupName,
    String groupCode,
    ElectiveSelectionMode selectionMode,
    Instant createdAt,
    Instant updatedAt
) {}
