package com.cms.dto;

import java.time.Instant;

public record CourseResponse(
    Long id,
    String name,
    String code,
    Integer credits,
    Integer theoryCredits,
    Integer labCredits,
    ProgramResponse program,
    Integer semester,
    Instant createdAt,
    Instant updatedAt
) {}
