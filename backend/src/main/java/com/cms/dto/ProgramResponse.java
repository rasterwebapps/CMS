package com.cms.dto;

import java.time.Instant;
import java.util.List;

import com.cms.model.enums.ProgramLevel;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    ProgramLevel programLevel,
    Integer durationYears,
    List<DepartmentResponse> departments,
    Instant createdAt,
    Instant updatedAt
) {}
