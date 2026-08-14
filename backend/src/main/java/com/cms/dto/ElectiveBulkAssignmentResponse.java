package com.cms.dto;

public record ElectiveBulkAssignmentResponse(
    int eligibleStudentCount,
    int assignedCount
) {}
