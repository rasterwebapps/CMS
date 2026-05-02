package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AdmissionRequest(
    @NotNull Long studentId,
    @NotNull Long joiningAcademicYearId,
    @NotNull LocalDate applicationDate,
    String declarationPlace,
    LocalDate declarationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven
) {}
