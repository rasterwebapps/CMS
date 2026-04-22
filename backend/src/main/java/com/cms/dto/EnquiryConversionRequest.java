package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.BloodGroup;
import com.cms.model.enums.CommunityCategory;
import com.cms.model.enums.Gender;

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
    Boolean applicantConsentGiven,

    // ── Student personal information ─────────────────────────────────────
    LocalDate dateOfBirth,
    Gender gender,
    String aadharNumber,

    // ── Student demographics ─────────────────────────────────────────────
    String nationality,
    String religion,
    CommunityCategory communityCategory,
    String caste,
    BloodGroup bloodGroup,

    // ── Student family information ───────────────────────────────────────
    String fatherName,
    String motherName,
    String parentMobile,

    // ── Student address ──────────────────────────────────────────────────
    AddressRequest address,

    // ── Admission declaration ────────────────────────────────────────────
    String declarationPlace,
    LocalDate declarationDate
) {}
