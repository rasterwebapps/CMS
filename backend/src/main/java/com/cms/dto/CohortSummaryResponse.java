package com.cms.dto;

import java.time.LocalDate;

public record CohortSummaryResponse(
    Long      id,
    String    cohortCode,
    String    displayName,
    String    courseName,
    String    courseCode,
    Integer   managementSeats,
    Integer   counsellingSeats,
    boolean   hasStudents,
    boolean   counsellingClosed,
    LocalDate counsellingClosedDate
) {}
