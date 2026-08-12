package com.cms.model.enums;

/**
 * Lifecycle of a special-class (or day-repeat) request. Null for {@link OccurrenceSource#REGULAR}
 * rows, which have no approval workflow.
 *
 * <ul>
 *   <li>{@code PENDING}   — Requested by faculty, awaiting admin review.</li>
 *   <li>{@code APPROVED}  — Admin approved; the session is live on the calendar.</li>
 *   <li>{@code REJECTED}  — Admin declined, with a reason.</li>
 *   <li>{@code CANCELLED} — Withdrawn after approval, before the occurrence date.</li>
 * </ul>
 */
public enum SpecialClassApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
