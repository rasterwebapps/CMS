package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.model.enums.SpecialClassApprovalStatus;

public interface SessionOccurrenceRepository extends JpaRepository<SessionOccurrence, Long> {

    Optional<SessionOccurrence> findByClassScheduleIdAndOccurrenceDate(Long classScheduleId, LocalDate occurrenceDate);

    long countByBatch_IdAndOccurrenceStatusNot(Long batchId, OccurrenceStatus occurrenceStatus);

    long countByCohortSection_IdAndOccurrenceStatusNot(Long cohortSectionId, OccurrenceStatus occurrenceStatus);

    List<SessionOccurrence> findByClassSchedule_CourseOffering_Id(Long courseOfferingId);

    /** Every occurrence riding on any of these ClassSchedule rows, regardless of date -- used by
     *  {@link com.cms.service.ClassScheduleCleanupService#purgeOccurrencesForCells} to find what
     *  has to be cleared before a hard delete of these cells. */
    List<SessionOccurrence> findByClassSchedule_IdIn(List<Long> classScheduleIds);

    /** The reverse side of a Phase 7 staff swap link -- who still points at these occurrences as
     *  their {@code swapPartnerOccurrence}. Used to unlink an external swap partner before a purge
     *  hard-deletes the occurrence it points at, since {@code swap_partner_occurrence_id} has no
     *  {@code ON DELETE} clause and would otherwise block the delete. */
    List<SessionOccurrence> findBySwapPartnerOccurrence_IdIn(List<Long> occurrenceIds);

    /** Cheap existence check for {@link com.cms.service.TimetableGenerationService#revertToDraft}'s
     *  guard: occurrences are only ever materialized against a PUBLISHED schedule (substitution,
     *  relocation, swap, or logged progress all gate on it), so any hit here means real activity is
     *  already on record for one of these cells and reverting it back to DRAFT would risk that
     *  history being silently swept away by a later purge -- same shape as the existing
     *  {@code labAttendanceRepository.existsByLabScheduleIdIn} guard right next to it. */
    boolean existsByClassSchedule_IdIn(List<Long> classScheduleIds);

    List<SessionOccurrence> findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
        Long termInstanceId, com.cms.model.enums.ClassScheduleStatus status);

    // ---- BR-55: special-class / day-repeat lookups. Additive only -- the three methods above,
    // all implicitly inner-joined through classSchedule, structurally can never see a row with a
    // null classSchedule, so they're untouched and continue to only surface REGULAR rows. ----

    /** Same-date/period conflict check against other special classes -- the one thing
     *  {@code ClassScheduleRepository.findOverlapping} can't see, since these rows have no
     *  ClassSchedule for it to match against. */
    List<SessionOccurrence> findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(
        List<OccurrenceSource> sources, LocalDate occurrenceDate, Long periodId);

    /** Faculty's own "My Special Classes" list. */
    List<SessionOccurrence> findByRequestedByFaculty_IdAndOccurrenceSourceInOrderByOccurrenceDateDesc(
        Long facultyId, List<OccurrenceSource> sources);

    /** Admin approval queue. */
    List<SessionOccurrence> findByApprovalStatusAndOccurrenceSourceInOrderByRequestedAtAsc(
        SpecialClassApprovalStatus approvalStatus, List<OccurrenceSource> sources);

    /** Fetch/bulk-act on every row of one DAY_REPEAT submission. */
    List<SessionOccurrence> findByRequestBatchId(UUID requestBatchId);

    /** Reserved for the progress-report-crediting fast-follow (BR-55, explicitly out of scope for
     *  v1) -- additive counterpart to {@link #findByClassSchedule_CourseOffering_Id}, which
     *  cannot see these rows either. Not yet called from any service. */
    List<SessionOccurrence> findByCourseOffering_Id(Long courseOfferingId);

    /** Every occurrence (any source -- REGULAR relocation, SPECIAL_CLASS, DAY_REPEAT) on one
     *  date, used by {@code RoomRelocationService}'s date-specific room-conflict check. */
    List<SessionOccurrence> findByOccurrenceDate(LocalDate occurrenceDate);

    /** Every REGULAR occurrence that Reschedule has moved onto a date within this window --
     *  {@code TimetableOccurrenceService.findOccurrences}' per-schedule natural-date walk can never
     *  discover these on its own, since by definition the target date isn't one of that schedule's
     *  own recurring dates; fetched directly by date range + status instead. */
    List<SessionOccurrence> findByOccurrenceStatusAndOccurrenceDateBetweenAndClassSchedule_TermInstance_IdAndClassSchedule_Status(
        OccurrenceStatus occurrenceStatus, LocalDate from, LocalDate to, Long termInstanceId, ClassScheduleStatus status);

    // ---- OC-175: CLINICAL_SHIFT idempotent-generation existence checks ----

    /** One CLINICAL_SHIFT row per (batch, date) -- the per-venue clinical block. */
    Optional<SessionOccurrence> findByOccurrenceSourceAndBatch_IdAndOccurrenceDate(
        OccurrenceSource occurrenceSource, Long batchId, LocalDate occurrenceDate);

    /** One CLINICAL_SHIFT row per (cohort section, subject, block start, date) -- the shared,
     *  reconvened theory block. */
    Optional<SessionOccurrence> findByOccurrenceSourceAndCohortSection_IdAndSubject_IdAndBlockStartTimeAndOccurrenceDate(
        OccurrenceSource occurrenceSource, Long cohortSectionId, Long subjectId, java.time.LocalTime blockStartTime,
        LocalDate occurrenceDate);
}
