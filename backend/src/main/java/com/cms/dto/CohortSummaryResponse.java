package com.cms.dto;

public record CohortSummaryResponse(
    Long    id,
    String  cohortCode,
    String  displayName,
    String  programName,
    String  programCode,
    Integer managementSeats,
    Integer counsellingSeats,
    boolean hasStudents
) {}
