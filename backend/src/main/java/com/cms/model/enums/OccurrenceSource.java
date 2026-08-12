package com.cms.model.enums;

/**
 * Discriminates what a {@link com.cms.model.SessionOccurrence} row actually represents.
 *
 * <ul>
 *   <li>{@code REGULAR}       — anchors an existing recurring {@link com.cms.model.ClassSchedule}
 *       row on a specific date (the original, pre-BR-55 purpose of this table).</li>
 *   <li>{@code SPECIAL_CLASS} — a single ad-hoc session with no backing ClassSchedule row.</li>
 *   <li>{@code DAY_REPEAT}    — one row of a whole-day-repeat batch (see
 *       {@link com.cms.model.SessionOccurrence#getRequestBatchId()}), also with no backing
 *       ClassSchedule row.</li>
 * </ul>
 */
public enum OccurrenceSource {
    REGULAR,
    SPECIAL_CLASS,
    DAY_REPEAT
}
