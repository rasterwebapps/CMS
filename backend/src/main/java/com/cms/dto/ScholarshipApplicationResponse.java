package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.DisbursementFrequency;
import com.cms.model.enums.ScholarshipApplicationMode;
import com.cms.model.enums.ScholarshipStatus;

public record ScholarshipApplicationResponse(
    Long id,
    Long studentId,
    String studentName,
    String rollNumber,
    Long programId,
    String programName,
    String programCode,
    Integer semester,
    Long scholarshipTypeId,
    String scholarshipCode,
    String scholarshipName,
    ScholarshipApplicationMode applicationMode,
    String portalName,
    Long academicYearId,
    String academicYearName,
    LocalDate applicationDate,
    String applicationRemarks,
    ScholarshipStatus status,
    String approvedBy,
    Instant approvedAt,
    String rejectionReason,
    BigDecimal approvedAmount,
    DisbursementFrequency disbursementFrequency,
    LocalDate validFrom,
    LocalDate validTill,
    // ── Govt sanction tracking ──
    String govtSanctionNumber,
    LocalDate sanctionDate,
    String sanctionedBy,
    Long renewedFromId,
    Boolean renewalRequired,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}

