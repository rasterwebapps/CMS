package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/** One shortfall unit {@link com.cms.service.TimetableGlobalAutoScheduleService} could not place.
 *  {@code courseOfferingId} is null only for a whole-elective-group failure (no single offering to
 *  point at) — used to deep-link a Special Class request pre-filled with the right subject. */
public record AutoPlaceUnplacedItem(
    String subjectName,
    ClassSessionType sessionType,
    String occupantLabel,
    String reason,
    Long courseOfferingId
) {}
