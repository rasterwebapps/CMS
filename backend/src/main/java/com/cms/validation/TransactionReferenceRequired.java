package com.cms.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that transactionReference is provided when payment mode
 * requires it (UPI, BANK_TRANSFER, or CHEQUE).
 * Apply this annotation at the class level.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionReferenceValidator.class)
@Documented
public @interface TransactionReferenceRequired {
    String message() default "Transaction reference is required for UPI, Bank Transfer, and Cheque payments";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

