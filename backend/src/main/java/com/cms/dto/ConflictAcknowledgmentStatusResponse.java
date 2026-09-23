package com.cms.dto;

import java.time.Instant;

/** Whether a term's Draft Review Approve action is currently unblocked by the Conflict Inspector
 *  acknowledgment gate (see V528) — {@code acknowledged} is false both when the term has never been
 *  acknowledged and when a prior acknowledgment was invalidated by a later skeleton change (see
 *  {@code TimetableConflictInspectorService#isAcknowledgmentValid}). */
public record ConflictAcknowledgmentStatusResponse(
    Long termInstanceId,
    boolean acknowledged,
    Instant acknowledgedAt
) {
}
