package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@TransactionReferenceRequired
public record TermFeePaymentRequest(
    @NotNull Long feeDemandId,
    @NotNull LocalDate paymentDate,
    @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
    @NotNull PaymentMode paymentMode,
    String transactionReference,
    String remarks
) {
    public TermFeePaymentRequest(Long feeDemandId, LocalDate paymentDate, BigDecimal amountPaid,
                                 PaymentMode paymentMode, String transactionReference) {
        this(feeDemandId, paymentDate, amountPaid, paymentMode, transactionReference, null);
    }
}
