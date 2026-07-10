package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@TransactionReferenceRequired
public record CollectPaymentRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    @NotNull(message = "Payment mode is required")
    PaymentMode paymentMode,

    String transactionReference,

    String remarks,

    /** Explicit receipt number to use. Null = auto-generate from the payment date's year sequence. */
    String receiptNumber,

    /**
     * Opt-in to collect more than total outstanding (bank-transfer/DD only, requires
     * FEE_COLLECT_EXCESS). The portion above outstanding becomes an auto-generated,
     * non-rejectable refund. Null/false = existing hard-capped behavior.
     */
    Boolean allowExcess
) {
    public boolean isAllowExcess() {
        return Boolean.TRUE.equals(allowExcess);
    }
}
