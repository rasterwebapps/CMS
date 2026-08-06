package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** R3 Phase 5 — assigns faculty + room to an already-placed skeleton cell. Subject/day/period/batch
 *  were fixed at placement time (Phase 4) and aren't touched here. classroomId is only read for an
 *  elective THEORY session (no single owning cohort, so still a free pick); every other case —
 *  non-elective THEORY, LAB, CLINICAL — resolves its room server-side from the venue already
 *  committed in Cohort Room Allocation and ignores this field entirely. */
public record StaffingAssignmentRequest(
    @NotNull(message = "Faculty is required")
    Long facultyId,

    /** Required only when the cell is an elective THEORY session. */
    Long classroomId
) {}
