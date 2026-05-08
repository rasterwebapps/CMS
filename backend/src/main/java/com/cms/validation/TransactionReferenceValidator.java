package com.cms.validation;

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.TermFeePaymentRequest;
import com.cms.model.enums.PaymentMode;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator that ensures transactionReference is provided when payment mode
 * requires a reference number: UPI, BANK_TRANSFER, CHEQUE, or DEMAND_DRAFT.
 */
public class TransactionReferenceValidator
    implements ConstraintValidator<TransactionReferenceRequired, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null checks
        }

        PaymentMode paymentMode = null;
        String transactionReference = null;

        // Extract fields based on the DTO type
        if (value instanceof EnquiryPaymentRequest req) {
            paymentMode = req.paymentMode();
            transactionReference = req.transactionReference();
        } else if (value instanceof TermFeePaymentRequest req) {
            paymentMode = req.paymentMode();
            transactionReference = req.transactionReference();
        } else if (value instanceof CollectPaymentRequest req) {
            paymentMode = req.paymentMode();
            transactionReference = req.transactionReference();
        }

        // If payment mode requires transaction reference, validate it's present
        if (requiresTransactionReference(paymentMode)) {
            return transactionReference != null && !transactionReference.trim().isEmpty();
        }

        return true;
    }

    private boolean requiresTransactionReference(PaymentMode mode) {
        return mode == PaymentMode.UPI
            || mode == PaymentMode.BANK_TRANSFER
            || mode == PaymentMode.CHEQUE
            || mode == PaymentMode.DEMAND_DRAFT;
    }
}

