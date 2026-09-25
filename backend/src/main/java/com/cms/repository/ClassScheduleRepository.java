package com.cms.repository;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.ClassSchedule;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long>, JpaSpecificationExecutor<ClassSchedule> {

    List<ClassSchedule> findByLabId(Long labId);

    List<ClassSchedule> findByLabIdAndIsActiveTrue(Long labId);

    List<ClassSchedule> findByFacultyId(Long facultyId);

    List<ClassSchedule> findByFacultyIdAndIsActiveTrue(Long facultyId);

    List<ClassSchedule> findByBatchName(String batchName);

    List<ClassSchedule> findByBatchNameAndIsActiveTrue(String batchName);

    long countByBatchIdAndIsActiveTrue(Long batchId);

    List<ClassSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<ClassSchedule> findByDayOfWeekAndIsActiveTrue(DayOfWeek dayOfWeek);

    List<ClassSchedule> findByTermInstanceId(Long termInstanceId);

    List<ClassSchedule> findByClassroomId(Long classroomId);

    List<ClassSchedule> findByClinicalVenueId(Long clinicalVenueId);

    List<ClassSchedule> findByPeriodId(Long periodId);

    List<ClassSchedule> findByCourseOfferingId(Long courseOfferingId);

    List<ClassSchedule> findByCourseOfferingIdAndIsActiveTrue(Long courseOfferingId);

    List<ClassSchedule> findBySessionTypeAndTermInstanceId(ClassSessionType sessionType, Long termInstanceId);

    List<ClassSchedule> findByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    /** Active rows only. Every Global Auto-Schedule rebuild now hard-deletes the previous run's
     *  unpinned DRAFT rows outright (see {@code TimetableGlobalAutoScheduleService#
     *  purgeDraftCellsForRebuild}), so a live {@code is_active = false} row should no longer be
     *  possible going forward -- but these filtered finders remain the correct pattern for
     *  anything that means "this term's timetable" (approve, the conflict scan, counts, the
     *  draft/published views), both defensively and for any environment still carrying leftover
     *  inactive copies from before that migration ran. Read through these, not the unfiltered
     *  finders above, or a stale copy gets acted on (and double-counted). */
    List<ClassSchedule> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);

    /** Active rows only, unscoped -- see the note on {@link #findByTermInstanceIdAndIsActiveTrue}. */
    List<ClassSchedule> findByIsActiveTrue();

    /** Cheap sibling of {@link #findByTermInstanceIdAndIsActiveTrue} used only to fingerprint "has
     *  the skeleton changed" for the Conflict Inspector acknowledgment gate (see
     *  TimetableConflictInspectorService#isAcknowledgmentValid) — no need to hydrate every row just
     *  to compare a count. */
    long countByTermInstanceIdAndIsActiveTrue(Long termInstanceId);

    /** The other half of that same fingerprint: the most recent edit timestamp across this term's
     *  active cells, compared against when the acknowledgment was recorded. Empty when the term has
     *  no active cells at all. */
    @Query("SELECT MAX(cs.updatedAt) FROM ClassSchedule cs WHERE cs.termInstance.id = :termInstanceId AND cs.isActive = true")
    Optional<Instant> findMaxUpdatedAtByTermInstanceIdAndIsActiveTrue(@Param("termInstanceId") Long termInstanceId);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndIsActiveTrue(Long termInstanceId, ClassScheduleStatus status);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndDayOfWeek(
        Long termInstanceId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    List<ClassSchedule> findByFacultyIdAndStatusAndDayOfWeek(
        Long facultyId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    List<ClassSchedule> findByFacultyIdAndStatusAndDayOfWeekAndIsActiveTrue(
        Long facultyId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    /** Every still-active PUBLISHED row sharing one Period+dayOfWeek slot, across all faculty --
     *  the period-scoped sibling of {@link #findByFacultyIdAndStatusAndDayOfWeek}, used by {@link
     *  com.cms.service.ClassScheduleOccurrenceService#schedulesDisruptedBy} to find every session a
     *  newly created BlockedPeriod actually cancels. */
    List<ClassSchedule> findByPeriodIdAndStatusAndDayOfWeekAndIsActiveTrue(
        Long periodId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndFacultyId(
        Long termInstanceId, ClassScheduleStatus status, Long facultyId);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndFacultyIdAndIsActiveTrue(
        Long termInstanceId, ClassScheduleStatus status, Long facultyId);

    List<ClassSchedule> findByTermInstanceIdAndFacultyIdAndStatusIn(
        Long termInstanceId, Long facultyId, List<ClassScheduleStatus> statuses);

    List<ClassSchedule> findByTermInstanceIdAndFacultyIdAndStatusInAndIsActiveTrue(
        Long termInstanceId, Long facultyId, List<ClassScheduleStatus> statuses);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndCourseOfferingIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> courseOfferingIds);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndCourseOfferingIdInAndIsActiveTrue(
        Long termInstanceId, ClassScheduleStatus status, List<Long> courseOfferingIds);

    /** Status-agnostic sibling of the method above — used by the cohort-wide Skeleton Builder,
     *  which (like {@link #findByCourseOfferingId}) needs already-published rows to still count
     *  as "placed" for budget/conflict purposes, not just DRAFT ones. */
    List<ClassSchedule> findByTermInstanceIdAndCourseOfferingIdIn(
        Long termInstanceId, List<Long> courseOfferingIds);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndBatchIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> batchIds);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndBatchIdInAndIsActiveTrue(
        Long termInstanceId, ClassScheduleStatus status, List<Long> batchIds);

    /** Every still-active row riding on one of these batches, regardless of status — used by
     *  {@link com.cms.service.CohortRoomAllocationService#revert} to find LAB/CLINICAL cells that
     *  would otherwise be orphaned (still isActive=true, still blocking conflict checks) once their
     *  batch is deactivated. */
    List<ClassSchedule> findByBatchIdInAndIsActiveTrue(List<Long> batchIds);

    /** THEORY sibling of {@link #findByBatchIdInAndIsActiveTrue} — THEORY cells carry a
     *  cohortSection reference instead of a batch. */
    List<ClassSchedule> findByCohortSectionIdInAndIsActiveTrue(List<Long> cohortSectionIds);

    /** OC-127 periodSpan: fetches every row of a multi-period session's linked group, in period
     *  order, so callers (staffing/removal) can treat them as one atomic unit. */
    List<ClassSchedule> findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(UUID sessionGroupId);

    boolean existsByTermInstanceId(Long termInstanceId);

    /** Subject-wide guard for {@code SubjectService#requireSafeToDeactivate} — a subject can have
     *  offerings across many terms, so this checks placed sessions across all of them at once
     *  rather than requiring a per-offering loop. */
    boolean existsByCourseOffering_Subject_Id(Long subjectId);

    boolean existsByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    /** Powers the Skeleton Builder's pre-run "you're about to overwrite what's already there"
     *  confirmation for an All-Cohorts Global Auto-Schedule run — {@code isActive = true} excludes
     *  rows an earlier rebuild already soft-deleted, so a stale deactivated DRAFT row never falsely
     *  triggers the warning. */
    boolean existsByTermInstanceIdAndStatusAndIsActiveTrue(Long termInstanceId, ClassScheduleStatus status);

    void deleteByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    void deleteByTermInstanceId(Long termInstanceId);

    /**
     * Resolves each candidate row to a concrete (startTime, endTime) via its Period — both THEORY
     * and LAB rows share the one Period master since V331 merged the formerly-separate LabSlot
     * master into it — and compares actual time-range overlap rather than slot-id equality — the
     * old lab_schedules conflict queries only matched on dayOfWeek+labSlot.id, which (a) missed
     * true overlaps between different slots with the same wall-clock time and (b) had no
     * term_instance_id scoping at all, so a slot in a *different* term with the same day/slot-id
     * used to falsely flag as conflicting. Both are fixed here as a byproduct of the redesign.
     */
    @Query("""
        SELECT cs FROM ClassSchedule cs
        WHERE cs.dayOfWeek = :dayOfWeek
          AND cs.termInstance.id = :termInstanceId
          AND cs.isActive = true
          AND cs.status = :status
          AND cs.id <> COALESCE(:excludeId, -1)
          AND cs.period IS NOT NULL
          AND cs.period.startTime < :endTime AND cs.period.endTime > :startTime
        """)
    List<ClassSchedule> findOverlapping(@Param("dayOfWeek") DayOfWeek dayOfWeek,
                                         @Param("termInstanceId") Long termInstanceId,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("status") ClassScheduleStatus status,
                                         @Param("excludeId") Long excludeId);

    /**
     * Used to hard-block a new FacultyAvailability block when it would collide with a class the
     * faculty member is already scheduled to teach. FacultyAvailability itself isn't term-scoped
     * (it's a standing weekly rule), so this checks across every non-LOCKED term instance -- a
     * PLANNED term (not yet open) can already have a fully-staffed skeleton, same as an OPEN one;
     * LOCKED is the only status the rest of the timetable engine already treats as frozen/immutable
     * (see TimetableGenerationService#requireNotLocked). Status-agnostic (DRAFT rows count too, not
     * just PUBLISHED) since an unreviewed DRAFT would still surface as a real conflict once
     * published.
     */
    @Query("""
        SELECT cs FROM ClassSchedule cs
        WHERE cs.faculty.id = :facultyId
          AND cs.dayOfWeek = :dayOfWeek
          AND cs.isActive = true
          AND cs.termInstance.status <> com.cms.model.enums.TermInstanceStatus.LOCKED
          AND cs.period IS NOT NULL
          AND cs.period.startTime < :endTime AND cs.period.endTime > :startTime
        """)
    List<ClassSchedule> findActiveConflictingForFaculty(@Param("facultyId") Long facultyId,
                                                          @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                          @Param("startTime") LocalTime startTime,
                                                          @Param("endTime") LocalTime endTime);
}
