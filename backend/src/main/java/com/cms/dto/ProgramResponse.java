package com.cms.dto;

import java.time.Instant;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    Integer durationYears,
    Instant createdAt,
    Instant updatedAt
) {}
