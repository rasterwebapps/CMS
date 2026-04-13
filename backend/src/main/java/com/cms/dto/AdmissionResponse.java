package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.AdmissionStatus;

public record AdmissionResponse(
    Long id,
    Long studentId,
    String studentName,
    Integer academicYearFrom,
    Integer academicYearTo,
    LocalDate applicationDate,
    AdmissionStatus status,
    String declarationPlace,
    LocalDate declarationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven,
    Instant createdAt,
    Instant updatedAt
) {}
