package com.cms.model.enums;

public enum OccurrenceStatus {
    HELD,
    SUBSTITUTED,
    CANCELLED,
    /** This occurrence was moved to a different date/period/room via Reschedule — a separate
     *  {@code SessionOccurrence} row (source REGULAR, occurrenceDate = the new date) carries the
     *  override; the original date's row is marked {@link #CANCELLED} with a remark explaining
     *  where it moved to. */
    RESCHEDULED
}
