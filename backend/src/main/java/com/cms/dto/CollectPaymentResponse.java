package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.PaymentMode;

public record CollectPaymentResponse(
    String receiptNumber,
    Long studentId,
    String studentName,
    String rollNumber,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    PaymentMode paymentMode,
    String transactionReference,
    String remarks,
    String allocationSummary,
    List<SemesterPaymentDetail> installmentBreakdown,
    /** TUITION_ONLY | TUITION_AND_HOSTEL */
    String feeCategory,
    Instant createdAt,
    /** Amount paid that exceeded all pending installments — zero when fully allocated */
    BigDecimal surplusAmount
) {}
