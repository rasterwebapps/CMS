package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.AdmissionStatus;

import jakarta.validation.constraints.NotNull;

public record AdmissionRequest(
    @NotNull Long studentId,
    @NotNull Integer academicYearFrom,
    @NotNull Integer academicYearTo,
    @NotNull LocalDate applicationDate,
    AdmissionStatus status,
    String declarationPlace,
    LocalDate declarationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven
) {}
