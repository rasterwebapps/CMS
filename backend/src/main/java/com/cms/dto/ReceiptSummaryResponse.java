package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.PaymentMode;

public record ReceiptSummaryResponse(
    String receiptNumber,
    Long studentId,
    String studentName,
    String rollNumber,
    BigDecimal totalAmountPaid,
    LocalDate paymentDate,
    PaymentMode paymentMode,
    String transactionReference,
    String remarks,
    String installmentsCovered,
    List<SemesterPaymentDetail> installmentBreakdown,
    Instant createdAt
) {}