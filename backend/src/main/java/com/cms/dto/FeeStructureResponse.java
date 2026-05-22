package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;

public record FeeStructureResponse(
    Long id,
    Long groupId,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Long academicYearId,
    String academicYearName,
    AdmissionQuota quota,
    Long feeStateId,
    String feeStateName,
    Gender gender,
    FeeType feeType,
    BigDecimal amount,
    String description,
    Boolean isMandatory,
    Boolean isActive,
    List<YearAmountResponse> yearAmounts,
    Instant createdAt,
    Instant updatedAt
) {}
