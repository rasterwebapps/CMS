package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AcademicYearResponse(
    Long id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    Instant createdAt,
    Instant updatedAt
) {}
