package com.cms.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One skeleton cell (R3 Phase 4) still waiting for faculty + room (R3 Phase 5). LAB/CLINICAL
 *  rows, and non-elective THEORY rows, carry the venue already committed in Cohort Room
 *  Allocation (Capacity Planner) — {@code venueId} null on one of those means it isn't committed
 *  yet and staffCell() will reject it; the frontend shows a fixed value, not a picker, for those.
 *  Elective THEORY rows ({@code isElective = true}) have no single owning cohort by design, so
 *  {@code venueId} is always null for them and the frontend keeps a free classroom pick.
 *  {@code rotatingBatchNames} is non-empty only for a cell that's part of a Rotation Group —
 *  {@code batchName} is null on those (there's no single fixed occupant), and the frontend shows
 *  "rotates: A / B" instead. */
public record UnstaffedCellResponse(
    Long id,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Long subjectSpecialityId,
    String subjectSpecialityName,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    Long periodId,
    String slotName,
    LocalTime startTime,
    LocalTime endTime,
    String batchName,
    Integer requiredStrength,
    Long venueId,
    String venueName,
    Integer venueCapacity,
    boolean isElective,
    List<String> rotatingBatchNames,

    /** OC-127 periodSpan: non-null only for a cell that's part of a multi-period session — every
     *  sibling row sharing this id is staffed/removed together as one atomic unit. */
    UUID sessionGroupId,

    /** The {@code CohortSection} this THEORY row was placed for, once its cohort's committed
     *  Theory room has been split into sections (see V368) -- null means "whole cohort" (no split)
     *  and, for LAB/CLINICAL, always null (their occupant is a {@code Batch} instead). Lets
     *  auto-staffing prefer this section's own {@code CourseOfferingSectionFaculty} override
     *  before falling back to the ranked department pool. */
    Long cohortSectionId
) {}
