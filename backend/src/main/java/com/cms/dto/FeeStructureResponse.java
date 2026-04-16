package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.cms.model.enums.FeeType;

public record FeeStructureResponse(
    Long id,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Long academicYearId,
    String academicYearName,
    FeeType feeType,
    BigDecimal amount,
    String description,
    Boolean isMandatory,
    Boolean isActive,
    List<YearAmountResponse> yearAmounts,
    Instant createdAt,
    Instant updatedAt
) {}
