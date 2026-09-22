package com.cms.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One placed cell in the skeleton grid — deliberately leaner than {@link ClassScheduleResponse}
 *  since the skeleton stage has no faculty/room yet; {@code isStaffed} is false until Phase 5's
 *  staffing pass fills those in and the row can be published. {@code rotationGroupLabel} is
 *  non-null only for a cell that's part of a Rotation Group — {@code batchId}/{@code batchName}
 *  are null on those (there's no single fixed occupant) and {@code rotatingBatchNames} lists who
 *  alternates through it instead. */
public record SkeletonCellResponse(
    Long id,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    Long periodId,
    String slotName,
    LocalTime startTime,
    LocalTime endTime,
    Long batchId,
    String batchName,
    Long cohortSectionId,
    String cohortSectionLabel,
    boolean isStaffed,
    ClassScheduleStatus status,
    String rotationGroupLabel,
    List<String> rotatingBatchNames,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Long electiveGroupId,
    String electiveGroupName,

    /** OC-127 periodSpan: non-null only for a cell that's part of a multi-period session — every
     *  sibling row sharing this id is placed/staffed/removed together as one atomic unit. */
    UUID sessionGroupId,

    /** True when a human positioned this cell on purpose, which makes it survive the next
     *  Global Auto-Schedule rebuild instead of being cleared with the rest of the DRAFT grid.
     *  Drives the pin affordance and badge in the Skeleton Builder. */
    boolean pinned,

    /** True for an institution-decided (management-selected) elective: only the chosen option
     *  runs, as a common cohort subject, so it moves, swaps and is replaced like any subject. False
     *  for a student-choice elective, whose options must keep sharing one slot. */
    boolean commonElective,

    /** True when this cell's subject is curriculum-typed {@code CO_CURRICULAR} (e.g. Self-Study) —
     *  advisory content the auto-scheduler always places last, never a real curriculum requirement
     *  (see {@code TimetableGlobalAutoScheduleService#isAdvisoryRow}). False for LIBRARY/SPORTS too
     *  (no {@code CourseOffering} to type at all), which the grid already colors as their own fixed
     *  categories rather than this one. Drives the Skeleton Builder grid's cell coloring. */
    boolean coCurricular
) {
    /** Pre-{@code coCurricular} shape, kept for every call site that predates the concept (chiefly
     *  this service's own large test suite) — defaults to {@code false}, the correct value for
     *  everything that isn't a curriculum-typed CO_CURRICULAR row anyway. Mirrors {@link
     *  SkeletonSubjectBudget}'s own two-constructor pattern for a field added after the fact. */
    public SkeletonCellResponse(Long id, ClassSessionType sessionType, DayOfWeek dayOfWeek, Long periodId,
            String slotName, LocalTime startTime, LocalTime endTime, Long batchId, String batchName,
            Long cohortSectionId, String cohortSectionLabel, boolean isStaffed, ClassScheduleStatus status,
            String rotationGroupLabel, List<String> rotatingBatchNames, Long courseOfferingId, String subjectName,
            String subjectCode, Long electiveGroupId, String electiveGroupName, UUID sessionGroupId, boolean pinned,
            boolean commonElective) {
        this(id, sessionType, dayOfWeek, periodId, slotName, startTime, endTime, batchId, batchName,
            cohortSectionId, cohortSectionLabel, isStaffed, status, rotationGroupLabel, rotatingBatchNames,
            courseOfferingId, subjectName, subjectCode, electiveGroupId, electiveGroupName, sessionGroupId, pinned,
            commonElective, false);
    }
}
