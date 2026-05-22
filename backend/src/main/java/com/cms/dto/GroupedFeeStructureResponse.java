package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;

public record GroupedFeeStructureResponse(
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
    BigDecimal totalAmount,
    List<FeeStructureResponse> items
) {}
