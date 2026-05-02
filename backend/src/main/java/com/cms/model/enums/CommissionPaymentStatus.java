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
    /** Some payouts recorded but commission not fully paid out. */
    PARTIAL,
    /** Total payouts equal the accrued commission amount. */
    PAID
}

