package com.cms.model.enums;

/**
 * Lifecycle of the commission payout to the agent / referral source for an
 * enquiry.  Independent of the student's fee payment status.
 */
public enum CommissionPaymentStatus {
    /** No commission applies to this enquiry. */
    NOT_APPLICABLE,
    /** Commission accrued but no payout recorded yet. */
    PENDING,
    /** A payment has been requested but not yet approved/recorded. */
    PAYMENT_REQUESTED,
    /** Some payouts recorded but commission not fully paid out. */
    PARTIAL,
    /** Total payouts equal the accrued commission amount. */
    PAID,
    /** Payment request has been transmitted to OneBook for online processing. */
    TRANSMITTED,
    /** OneBook has accepted and is processing the payment. */
    PROCESSING,
    /** OneBook reported a payment failure; retry or handle manually. */
    FAILED
}

