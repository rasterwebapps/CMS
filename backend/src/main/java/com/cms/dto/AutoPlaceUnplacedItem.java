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
 *  still reported, but false: another working day fixes none of them. */
public record AutoPlaceUnplacedItem(
    String subjectName,
    ClassSessionType sessionType,
    String occupantLabel,
    String reason,
    Long courseOfferingId,
    boolean slotShortfall
) {}
