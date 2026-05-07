package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ScholarshipEligibilityRequest(
    Boolean isFirstGraduate,
    Boolean isMeritBased,
    Boolean isSportsQuota,
    Boolean isEconomicallyWeaker,

    @PositiveOrZero(message = "Annual family income must be zero or positive")
    BigDecimal annualFamilyIncome,
    String incomeCertificateNumber,
    String incomeCertIssuingAuthority,
    LocalDate incomeCertIssueDate,

    String communityCertificateNumber,
    String commCertIssuingAuthority,
    LocalDate commCertIssueDate,

    String firstGraduateCertificateNumber,
    String firstGradCertIssuingAuthority,
    LocalDate firstGradCertIssueDate,

    String fatherEducation,
    String motherEducation,

    // ── DBT (Direct Benefit Transfer) ────────────────────────────────────
    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar number must be exactly 12 digits")
    String aadhaarNumber,

    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    String bankAccountNumber,

    @Pattern(regexp = "^$|^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC format (e.g. SBIN0001234)")
    String bankIfsc,

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    String bankName,

    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
    String bankBranch,

    Boolean dbtLinked
) {}

