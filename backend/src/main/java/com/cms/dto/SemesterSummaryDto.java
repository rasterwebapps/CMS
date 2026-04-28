package com.cms.dto;

import java.math.BigDecimal;

public record SemesterSummaryDto(
    Long termInstanceId,
    String termInstanceLabel,
    Long cohortId,
    String cohortCode,
    int totalStudents,
    int passCount,
    int failCount,
    BigDecimal averagePercentage
) {}
