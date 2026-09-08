package com.cms.inventory.approval.model.enums;

/**
 * Structured reason for bypassing an approval step — same shape as {@code
 * WantedListRejectionReason} (a fixed taxonomy plus free-text notes on the action itself),
 * covering the ERP-standard exception categories the plan's own text named. See the "Exception
 * handling slice" decision-log entry.
 */
public enum ApprovalExceptionReason {
    URGENT_PURCHASE,
    SINGLE_SUPPLIER_SITUATION,
    EMERGENCY,
    APPROVER_UNAVAILABLE,
    OTHER
}
