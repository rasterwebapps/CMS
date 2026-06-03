package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LegacyAdmitRequest(

    // ── Student identity ─────────────────────────────────────────────────
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    String phone,

    // ── Admission context ────────────────────────────────────────────────
    @NotNull Long programId,
    Long courseId,
    @NotNull Long joiningAcademicYearId,
    @NotNull LocalDate admissionDate,
    @NotNull LocalDate applicationDate,
    @NotNull Integer yearOfStudy,
    AdmissionQuota admissionQuota,
    StudentType studentType,

    // ── Personal information ─────────────────────────────────────────────
    LocalDate dateOfBirth,
    Gender gender,
    String aadharNumber,

    // ── Demographics ─────────────────────────────────────────────────────
    String nationality,
    String religion,
    String communityCategory,
    String caste,
    String bloodGroup,
    Boolean physicalDisability,

    // ── Family information ───────────────────────────────────────────────
    String fatherName,
    String fatherPhone,
    String fatherEmail,
    String motherName,
    String motherPhone,
    String motherEmail,
    String parentMobile,

    // ── Address ──────────────────────────────────────────────────────────
    AddressRequest address,

    // ── Declaration ──────────────────────────────────────────────────────
    String declarationPlace,
    LocalDate declarationDate,

    // ── Referral (all optional — defaults to WALK_IN) ────────────────────
    Long referralTypeId,
    Long agentId,
    BigDecimal commissionAmount,
    Long referredStudentId,
    Long referredFacultyId,

    // ── Fee history ───────────────────────────────────────────────────────
    @Valid List<LegacyYearFeeEntry> yearFees,
    @Valid List<LegacyPaymentEntry> payments

) {}
