package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;

public record TermInstanceDto(
    Long id,
    Long academicYearId,
    String academicYearName,
    TermType termType,
    LocalDate startDate,
    LocalDate endDate,
    TermInstanceStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
