package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.Gender;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnquiryConversionRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    String phone,
    @JsonAlias("yearOfStudy") @NotNull Integer semester,
    @NotNull LocalDate admissionDate,
    @NotNull Long joiningAcademicYearId,
    @NotNull LocalDate applicationDate,
    Boolean parentConsentGiven,
    Boolean applicantConsentGiven,

    // ── Student personal information ─────────────────────────────────────
    LocalDate dateOfBirth,
    Gender gender,
    String aadharNumber,

    // ── Student demographics ─────────────────────────────────────────────
    String nationality,
    String religion,
    String communityCategory,
    String caste,
    String bloodGroup,

    // ── Student family information ───────────────────────────────────────
    String fatherName,
    String fatherPhone,
    String fatherEmail,
    String motherName,
    String motherPhone,
    String motherEmail,
    String parentMobile,

    // ── Student address ──────────────────────────────────────────────────
    AddressRequest address,

    // ── Admission declaration ────────────────────────────────────────────
    String declarationPlace,
    LocalDate declarationDate
) {}
