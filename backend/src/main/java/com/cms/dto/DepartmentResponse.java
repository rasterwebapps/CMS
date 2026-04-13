package com.cms.dto;

import java.time.Instant;

public record DepartmentResponse(
    Long id,
    String name,
    String code,
    String description,
    String hodName,
    Instant createdAt,
    Instant updatedAt
) {}
