package com.cms.dto;

public record ImportDefaultsRequest(
    Long    defaultJoiningAcademicYearId,
    String  defaultStudentType,        // "DAY_SCHOLAR" | "HOSTELER"
    String  defaultNationality,
    String  defaultState,
    Integer defaultSemester,           // default 1
    String  defaultAdmissionCategory,  // "MANAGEMENT" | "COUNSELLING"
    Boolean skipErroredRows            // continue on row-level errors
) {}
