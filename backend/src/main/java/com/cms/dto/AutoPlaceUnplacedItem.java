package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/** One shortfall unit {@link com.cms.service.TimetableGlobalAutoScheduleService} could not place.
 *  {@code courseOfferingId} is null only for a whole-elective-group failure (no single offering to
 *  point at) — used to deep-link a Special Class request pre-filled with the right subject.
 *
 * <p>{@code slotShortfall} is true only when curriculum hours went unplaced because the week had no
 *  free slot left for them — the one case more working Saturdays or a Special Class can close, and
 *  the only kind the report's "didn't have room" alert counts. Library and idle-batch fallbacks,
 *  Self-Study/gap-fill period notes, a missing faculty or elective selection, and a room ceiling are
 *  still reported, but false: another working day fixes none of them.
 *
 * <p>{@code advisoryOnly} (OC-256 follow-up, 2026-09-17) is a different axis entirely: whether this
 *  item represents real curriculum Theory/Lab/Clinical hours the cohort still owes (false — a
 *  genuine gap, e.g. "no faculty assigned" or "0.8h still unplaced") versus Library/Sports/Self-
 *  Study/idle-batch filler that has no curriculum-hours budget at all and was never required to
 *  place (true). Both render in {@code unplaced} today with identical alarming styling, which is
 *  exactly what made a month of genuinely-complete runs read as broken — {@code
 *  TimetableCoverageCalculator}/{@code TimetableGenerationService#approve} already agree Library/
 *  Sports/Self-Study don't gate anything; the frontend must now visually agree too (advisory items
 *  render muted, like {@code infoNotes}, not as a warning). NOT derivable from {@code sessionType}
 *  alone — Self-Study/Gap-Fill filler is tagged THEORY (it fills a Theory-shaped slot) despite being
 *  advisory, so this is set explicitly at each construction site instead. */
public record AutoPlaceUnplacedItem(
    String subjectName,
    ClassSessionType sessionType,
    String occupantLabel,
    String reason,
    Long courseOfferingId,
    boolean slotShortfall,
    boolean advisoryOnly
) {}
