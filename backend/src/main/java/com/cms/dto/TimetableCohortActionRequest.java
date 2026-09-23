package com.cms.dto;

import java.util.List;

/** Body for {@code POST /timetables/{termInstanceId}/revert-to-draft} and {@code
 *  .../discard-draft} (OC-260) — both became cohort-scoped, so the caller must always name exactly
 *  which cohort(s) the action applies to. */
public record TimetableCohortActionRequest(
    List<Long> cohortIds
) {}
