package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnquiryConversionRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    String phone,
    @NotNull Integer semester,
    @NotNull LocalDate admissionDate,
    @NotNull Integer academicYearFrom,
    @NotNull Integer academicYearTo,
    @NotNull LocalDate applicationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven
) {}
