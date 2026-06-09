package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EnquiryCreditApplicationDto(
    Long id,
    Long enquiryId,
    String enquiryName,
    Long studentId,
    String studentName,
    String rollNumber,
    Long semesterFeeId,
    String semesterLabel,
    BigDecimal amountApplied,
    String receiptNumber,
    Instant appliedAt
) {}
