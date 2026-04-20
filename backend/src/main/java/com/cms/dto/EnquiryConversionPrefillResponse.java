package com.cms.dto;

public record EnquiryConversionPrefillResponse(
    String firstName,
    String lastName,
    String email,
    String phone,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Integer suggestedSemester
) {}
