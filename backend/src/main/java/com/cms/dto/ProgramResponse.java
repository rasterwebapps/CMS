package com.cms.dto;

import java.time.Instant;
import java.util.Set;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.ProgramStatus;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    Integer durationYears,
    Integer totalSemesters,
    ProgramStatus status,
    Set<DocumentType> requiredDocumentTypes,
    Instant createdAt,
    Instant updatedAt
) {}
