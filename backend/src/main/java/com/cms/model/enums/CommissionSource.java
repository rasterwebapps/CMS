package com.cms.model.enums;

/**
 * Where the commission amount on an enquiry was sourced from.
 *
 * <ul>
 *   <li>{@link #AGENT}         – the linked agent has its own override
 *                                (Agent.commissionAmount &gt; 0)</li>
 *   <li>{@link #REFERRAL_TYPE} – the referral type carries the commission</li>
 *   <li>{@link #NONE}          – no commission applies</li>
 * </ul>
 */
public enum CommissionSource {
    AGENT,
    STAFF_REFERRER,
    FACULTY_REFERRER,
    REFERRAL_TYPE,
    NONE
}

