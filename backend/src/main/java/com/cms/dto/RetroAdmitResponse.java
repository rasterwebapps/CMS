package com.cms.dto;

import java.math.BigDecimal;

public record RetroAdmitResponse(
    Long studentId,
    String admissionNumber,
    String studentName,
    String rollNumber,
    Long enquiryId,
    int yearsWithFeeRecords,
    int paymentRowsCreated,
    BigDecimal totalHistoricalPaid
) {}
