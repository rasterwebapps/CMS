package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AdmissionResponse(
    Long id,
    Long studentId,
    String studentName,
    String admissionNumber,
    String rollNumber,
    String programName,
    String courseName,
    Integer semester,
    String studentStatus,
    Long joiningAcademicYearId,
    String joiningAcademicYearName,
    Integer expectedCompletionYear,
    LocalDate applicationDate,
    String declarationPlace,
    LocalDate declarationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven,
    Instant createdAt,
    Instant updatedAt
) {}
