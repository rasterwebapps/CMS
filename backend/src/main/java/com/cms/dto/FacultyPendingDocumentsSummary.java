package com.cms.dto;

public record FacultyPendingDocumentsSummary(
    Long facultyId,
    String fullName,
    String employeeCode,
    String departmentName,
    String designation,
    int pendingCount
) {}
