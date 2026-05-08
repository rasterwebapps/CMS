package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.DisbursementMode;

public record DisbursementResponse(
    Long id,
    Long studentScholarshipId,
    Long studentId,
    String studentName,
    Long academicYearId,
    String academicYearName,
    Integer termNumber,
    BigDecimal amount,
    LocalDate disbursementDate,
    DisbursementMode disbursementMode,
    String transactionReference,
    String chequeNumber,
    String bankName,
    String remarks,
    String disbursedBy,
    Instant createdAt
) {}

