package com.cms.inventory.issue.model.enums;

/**
 * Simple two-state lifecycle — issuing and returning are each a single direct action, no
 * DRAFT/submit workflow (nothing to build up first, unlike {@code StockIssueRequest}). "Overdue"
 * is deliberately not a stored state here — it's derived at read time from {@code
 * expectedReturnDate} vs. today, so it's never stale. See the "Loanable Item Issue slice"
 * decision-log entry.
 */
public enum LoanableItemIssueStatus {
    ISSUED,
    RETURNED
}
