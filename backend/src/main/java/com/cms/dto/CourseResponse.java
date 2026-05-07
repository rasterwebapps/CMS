package com.cms.dto;

import java.time.Instant;

public record CourseResponse(
    Long id,
    String name,
    String code,
    String specialization,
    String rollNumberCode,
    ProgramResponse program,
    Instant createdAt,
    Instant updatedAt
) {}
