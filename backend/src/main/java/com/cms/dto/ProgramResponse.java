package com.cms.dto;

import java.time.Instant;
import java.util.Set;

import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.ProgramStatus;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    Integer durationYears,
    Integer totalTerms,
    ProgramStatus status,
    AssessmentPattern assessmentPattern,
    Set<DocumentType> requiredDocumentTypes,
    Instant createdAt,
    Instant updatedAt
) {}
