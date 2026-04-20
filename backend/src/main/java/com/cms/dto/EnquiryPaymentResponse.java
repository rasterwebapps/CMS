package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

public record EnquiryPaymentResponse(
    Long id,
    Long enquiryId,
    String enquiryName,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    PaymentMode paymentMode,
    String transactionReference,
    String remarks,
    String receiptNumber,
    String collectedBy,
    String newStatus,
    Instant createdAt
) {}
