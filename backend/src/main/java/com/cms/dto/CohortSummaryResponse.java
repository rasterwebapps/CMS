package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CohortSummaryResponse(
    Long        id,
    String      cohortCode,
    String      displayName,
    String      courseName,
    String      courseCode,
    Integer     totalSeats,
    BigDecimal  managementPercentage,
    Integer     managementSeats,
    Integer     counsellingSeats,
    boolean     hasStudents,
    boolean     counsellingClosed,
    LocalDate   counsellingClosedDate,
    boolean     managementClosed,
    LocalDate   managementClosedDate
) {}
