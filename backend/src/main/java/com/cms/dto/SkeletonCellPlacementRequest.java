package com.cms.dto;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** No faculty/room fields on purpose — the skeleton stage (R3 Phase 4) only places
 *  period+type+subject; Phase 5's staffing pass fills in faculty/room afterward. */
public record SkeletonCellPlacementRequest(
    @NotNull(message = "Course offering is required")
    Long courseOfferingId,

    @NotNull(message = "Session type is required")
    ClassSessionType sessionType,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Period is required")
    Long periodId,

    /** Required for LAB/CLINICAL (each batch needs its own placement); optional for THEORY
     *  (R3 Phase 3 section-scoping — null means the whole cohort). */
    Long batchId,

    /** Passed explicitly rather than inferred — CourseOffering has no enforced Cohort FK (only
     *  resolvable via curriculumVersion.course matching), and the caller already has the cohort
     *  selected. Used to find sibling offerings for the cohort-exclusivity Theory conflict check. */
    @NotNull(message = "Cohort is required")
    Long cohortId,

    /** THEORY only — selects which CohortSection this session is for, once the cohort's
     *  committed Theory room allocation has one or more active sections (required whenever any
     *  exist, even the trivial single-section case, so Staffing can resolve the room directly
     *  later). Ignored for LAB/CLINICAL — their section scope is derived from the chosen
     *  batchId's own Batch.cohortSection instead. */
    Long cohortSectionId
) {}
