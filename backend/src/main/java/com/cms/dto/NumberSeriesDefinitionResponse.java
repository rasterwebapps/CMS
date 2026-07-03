package com.cms.dto;

import java.time.Instant;

public record NumberSeriesDefinitionResponse(
    Long id,
    String seriesCode,
    String seriesName,
    String scopeType,
    String prefix,
    String separator,
    int sequencePadding,
    String description,
    boolean active,
    boolean canEditScopeType,

    // Current-period counter data (resolved at query time from app timezone)
    String currentPeriodLabel,
    int currentLastSequence,
    String currentLastGenerated,  // null for multi-scope types (COURSE, ACADEMIC_YEAR_COURSE)
    String currentNextPreview,    // null for multi-scope types

    Instant createdAt,
    Instant updatedAt
) {}
