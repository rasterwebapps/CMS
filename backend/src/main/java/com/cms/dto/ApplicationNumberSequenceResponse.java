package com.cms.dto;

import java.time.Instant;

public record ApplicationNumberSequenceResponse(
    Long id,
    String seriesCode,
    String seriesName,
    String scopeType,
    String scopeKey,
    String prefix,
    Integer sequencePadding,
    Integer lastSequence,
    String lastGeneratedNumber,
    String nextPreviewNumber,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
