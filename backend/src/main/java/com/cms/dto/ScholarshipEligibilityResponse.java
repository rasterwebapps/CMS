package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ScholarshipEligibilityResponse(
    Long id,
    Long studentId,
    String studentName,
    String communityCategory,
    String caste,
    Boolean isFirstGraduate,
    Boolean isMeritBased,
    Boolean isSportsQuota,
    Boolean isEconomicallyWeaker,
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
    String verifiedBy,
    Instant verifiedAt,
    String verificationRemarks,// DBT details — Aadhaar is returned MASKED ("XXXXXXXX1234") for security
    String aadhaarNumberMasked,
    String bankAccountNumber,
    String bankIfsc,
    String bankName,
    String bankBranch,
    Boolean dbtLinked,
    List<ScholarshipTypeResponse> eligibleScholarships,
    Instant createdAt,
    Instant updatedAt
) {}

