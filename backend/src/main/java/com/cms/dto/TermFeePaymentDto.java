package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.PaymentMode;

public record TermFeePaymentDto(
    Long id,
    Long feeDemandId,
    String studentName,
    LocalDate paymentDate,
    BigDecimal amountPaid,
    BigDecimal lateFeeApplied,
    BigDecimal totalCollected,
    PaymentMode paymentMode,
    String receiptNumber,
    String transactionReference,
    String remarks,
    DemandStatus demandStatus,
    Instant createdAt,
    Instant updatedAt
) {
    public TermFeePaymentDto(Long id, Long feeDemandId, String studentName, LocalDate paymentDate,
                             BigDecimal amountPaid, BigDecimal lateFeeApplied, BigDecimal totalCollected,
                             PaymentMode paymentMode, String receiptNumber, String transactionReference,
                             DemandStatus demandStatus, Instant createdAt, Instant updatedAt) {
        this(id, feeDemandId, studentName, paymentDate, amountPaid, lateFeeApplied, totalCollected,
            paymentMode, receiptNumber, transactionReference, null, demandStatus, createdAt, updatedAt);
    }
}
