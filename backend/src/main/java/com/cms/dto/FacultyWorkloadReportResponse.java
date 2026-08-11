package com.cms.dto;

import java.util.List;

public record FacultyWorkloadReportResponse(
    Long termInstanceId,
    List<FacultyWorkloadRow> rows,
    double totalDemandHoursPerWeek,
    double totalCommittedHoursPerWeek,
    double totalConfiguredCapacityHoursPerWeek,
    int unconfiguredFacultyCount
) {}
