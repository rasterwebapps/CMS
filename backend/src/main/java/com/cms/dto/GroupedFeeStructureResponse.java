package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record GroupedFeeStructureResponse(
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Long academicYearId,
    String academicYearName,
    BigDecimal totalAmount,
    List<FeeStructureResponse> items
) {}
