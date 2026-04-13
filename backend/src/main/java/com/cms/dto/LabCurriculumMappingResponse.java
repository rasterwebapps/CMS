package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.MappingLevel;
import com.cms.model.enums.OutcomeType;

public record LabCurriculumMappingResponse(
    Long id,
    Long experimentId,
    String experimentName,
    Integer experimentNumber,
    Long courseId,
    String courseName,
    OutcomeType outcomeType,
    String outcomeCode,
    String outcomeDescription,
    MappingLevel mappingLevel,
    String justification,
    Instant createdAt,
    Instant updatedAt
) {}
