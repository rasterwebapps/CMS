package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.FeeType;

public record FeeStructureResponse(
    Long id,
    Long programId,
    String programName,
    Long academicYearId,
    String academicYearName,
    FeeType feeType,
    BigDecimal amount,
    String description,
    Boolean isMandatory,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
