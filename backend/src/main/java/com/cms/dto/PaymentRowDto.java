package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

public record PaymentRowDto(
    Long id,
    LocalDate paymentDate,
    BigDecimal amountPaid,
    BigDecimal lateFeeApplied,
    BigDecimal totalCollected,
    PaymentMode paymentMode,
    String receiptNumber,
    String transactionReference,
    String remarks
) {}
