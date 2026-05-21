package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;

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
    LocalDate suggestedApplicationDate,
    LocalDate dateOfBirth,
    Gender gender,
    StudentType studentType,
    Long countryId,
    String countryName,
    String state,
    String district,
    String remarks
) {}
