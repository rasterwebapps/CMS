package com.cms.dto;

public record CohortSummaryResponse(
    Long    id,
    String  cohortCode,
    String  displayName,
    String  courseName,
    String  courseCode,
    Integer managementSeats,
    Integer counsellingSeats,
    boolean hasStudents
) {}
