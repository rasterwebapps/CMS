package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@TransactionReferenceRequired
public record EnquiryPaymentRequest(
    @NotNull @Positive BigDecimal amountPaid,
    @NotNull LocalDate paymentDate,
    @NotNull PaymentMode paymentMode,
    String transactionReference,
    String remarks,

    /**
     * Opt-in to collect beyond the currently-open terms' outstanding, up to the enquiry's full
     * remaining course fee (any payment mode). Requires ENQUIRY_FEE_COLLECT_ADVANCE.
     */
    Boolean allowAdvance,

    /**
     * Opt-in to exceed even the full course fee (demand-draft/bank-transfer only, requires
     * ENQUIRY_FEE_COLLECT_ADVANCE and allowAdvance). The portion above the full course fee
     * becomes an auto-generated, non-rejectable refund.
     */
    Boolean allowExcess
) {
    public boolean isAllowAdvance() {
        return Boolean.TRUE.equals(allowAdvance);
    }

    public boolean isAllowExcess() {
        return Boolean.TRUE.equals(allowExcess);
    }
}
