package com.cms.dto;

import java.util.List;

/** Per-offering roll-up of every {@code CourseOfferingSectionFaculty} row currently on file --
 *  backs the Assign Faculty list table's Faculty column, which can no longer show a single
 *  offering-wide name now that assignment is per-cohort (per-section, if split). {@code
 *  assignedFacultyNames} is deduplicated and sorted; an offering with no rows at all has an empty
 *  list (rendered as "Unassigned" by the caller) -- a row is only ever persisted once a faculty is
 *  actually picked, so every name here is a real, currently-saved assignment, not a placeholder. */
public record CourseOfferingFacultySummaryDto(
    Long offeringId,
    List<String> assignedFacultyNames
) {}
