package com.cms.dto;

public record CourseOfferingUpdateRequest(
    Long facultyId,
    /** Informational-only backup/co-instructor note — never eligible for staffing/substitution. */
    Long secondaryFacultyId
) {}
