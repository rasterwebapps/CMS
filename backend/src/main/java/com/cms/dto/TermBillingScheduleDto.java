package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;

public record TermBillingScheduleDto(
    Long id,
    Long academicYearId,
    String academicYearName,
    TermType termType,
    LocalDate dueDate,
    LateFeeType lateFeeType,
    BigDecimal lateFeeAmount,
    Integer graceDays,
    Instant createdAt,
    Instant updatedAt
) {}
