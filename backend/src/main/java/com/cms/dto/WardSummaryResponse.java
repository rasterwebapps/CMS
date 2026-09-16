package com.cms.dto;

/** A lightweight summary of one ward (student) as seen from a guardian's self-service view --
 *  just enough to power a ward switcher, never the full Student record. */
public record WardSummaryResponse(
    Long studentId,
    String fullName,
    String rollNumber,
    boolean isPrimary
) {}
