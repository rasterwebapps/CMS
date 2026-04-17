package com.cms.dto;

import java.time.Instant;
import java.util.List;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    Integer durationYears,
    List<DepartmentResponse> departments,
    Instant createdAt,
    Instant updatedAt
) {}
