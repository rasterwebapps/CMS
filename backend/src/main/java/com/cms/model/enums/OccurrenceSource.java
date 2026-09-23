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
 *   <li>{@code CLINICAL_SHIFT} — one block of a {@link com.cms.model.ClinicalShiftGroup}: either a
 *       CLINICAL block (one row per linked {@link com.cms.model.Batch}, its own off-campus venue)
 *       or a shared THEORY block (one row, {@code cohortSection}-scoped — the reconvened full
 *       roster). No backing ClassSchedule row and no {@code period} — the shift's real clock times
 *       carry directly on {@code blockStartTime}/{@code blockEndTime} instead.</li>
 *   <li>{@code RECURRING_SPECIAL_CLASS} — one row of a weekly-recurring special-class batch (same
 *       {@code requestBatchId} grouping as {@code DAY_REPEAT}): the same subject/venue/faculty
 *       request repeated every week, same weekday, between a start and end date, skipping any week
 *       whose date isn't itself a non-instruction day (see {@code
 *       SpecialClassRequestService#requireNonInstructionDay}). Otherwise shaped exactly like
 *       {@code SPECIAL_CLASS}.</li>
 * </ul>
 */
public enum OccurrenceSource {
    REGULAR,
    SPECIAL_CLASS,
    DAY_REPEAT,
    CLINICAL_SHIFT,
    RECURRING_SPECIAL_CLASS
}
