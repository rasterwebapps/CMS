package com.cms.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.ClassSchedule;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByLabId(Long labId);

    List<ClassSchedule> findByFacultyId(Long facultyId);

    List<ClassSchedule> findByBatchName(String batchName);

    List<ClassSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<ClassSchedule> findByTermInstanceId(Long termInstanceId);

    List<ClassSchedule> findByClassroomId(Long classroomId);

    List<ClassSchedule> findByClinicalVenueId(Long clinicalVenueId);

    List<ClassSchedule> findByPeriodId(Long periodId);

    List<ClassSchedule> findByCourseOfferingId(Long courseOfferingId);

    List<ClassSchedule> findBySessionTypeAndTermInstanceId(ClassSessionType sessionType, Long termInstanceId);

    List<ClassSchedule> findByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndDayOfWeek(
        Long termInstanceId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    List<ClassSchedule> findByFacultyIdAndStatusAndDayOfWeek(
        Long facultyId, ClassScheduleStatus status, DayOfWeek dayOfWeek);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndFacultyId(
        Long termInstanceId, ClassScheduleStatus status, Long facultyId);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndCourseOfferingIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> courseOfferingIds);

    /** Status-agnostic sibling of the method above — used by the cohort-wide Skeleton Builder,
     *  which (like {@link #findByCourseOfferingId}) needs already-published rows to still count
     *  as "placed" for budget/conflict purposes, not just DRAFT ones. */
    List<ClassSchedule> findByTermInstanceIdAndCourseOfferingIdIn(
        Long termInstanceId, List<Long> courseOfferingIds);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndBatchIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> batchIds);

    /** OC-127 periodSpan: fetches every row of a multi-period session's linked group, in period
     *  order, so callers (staffing/removal) can treat them as one atomic unit. */
    List<ClassSchedule> findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(UUID sessionGroupId);

    boolean existsByTermInstanceId(Long termInstanceId);

    boolean existsByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

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
