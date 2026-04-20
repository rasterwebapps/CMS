package com.cms.dto;

import java.time.LocalDate;

public record EnquiryConversionPrefillResponse(
    String firstName,
    String lastName,
    String email,
    String phone,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Integer suggestedSemester,
    Integer suggestedAcademicYearFrom,
    Integer suggestedAcademicYearTo,
    LocalDate suggestedApplicationDate
) {}
