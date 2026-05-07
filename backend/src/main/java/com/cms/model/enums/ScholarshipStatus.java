package com.cms.model.enums;

/**
 * Lifecycle of a scholarship application.
 *
 * <ul>
 *   <li>{@code PENDING}    — Just applied, awaiting institution review.</li>
 *   <li>{@code APPROVED}   — Institution has approved the application and recorded the amount.</li>
 *   <li>{@code SANCTIONED} — For govt-portal schemes (NSP, ePass TN, TNSMS): the government
 *       has sanctioned the amount in their portal and provided a sanction number. Stronger
 *       guarantee than APPROVED — money will be credited to the student via DBT.</li>
 *   <li>{@code REJECTED}   — Application turned down with a reason.</li>
 *   <li>{@code ON_HOLD}    — Temporarily paused (e.g. document re-submission required).</li>
 *   <li>{@code CANCELLED}  — Withdrawn or invalidated.</li>
 * </ul>
 */
public enum ScholarshipStatus {
    PENDING,
    APPROVED,
    SANCTIONED,
    REJECTED,
    ON_HOLD,
    CANCELLED
}

