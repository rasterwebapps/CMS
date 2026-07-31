package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** R3 Phase 5 — assigns faculty + the type-appropriate room to an already-placed skeleton cell.
 *  Subject/day/period/batch were fixed at placement time (Phase 4) and aren't touched here; only
 *  one of classroomId/labId/clinicalVenueId is required, matching the cell's own session type. */
public record StaffingAssignmentRequest(
    @NotNull(message = "Faculty is required")
    Long facultyId,

    /** Required when the cell's sessionType = THEORY. */
    Long classroomId,

    /** Required when the cell's sessionType = LAB. */
    Long labId,

    /** Required when the cell's sessionType = CLINICAL. */
    Long clinicalVenueId
) {}
