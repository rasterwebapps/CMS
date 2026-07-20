package com.cms.repository;

import java.time.LocalTime;
import java.util.List;

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

    List<ClassSchedule> findByPeriodId(Long periodId);

    List<ClassSchedule> findByCourseOfferingId(Long courseOfferingId);

    List<ClassSchedule> findBySessionTypeAndTermInstanceId(ClassSessionType sessionType, Long termInstanceId);

    List<ClassSchedule> findByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndFacultyId(
        Long termInstanceId, ClassScheduleStatus status, Long facultyId);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndCourseOfferingIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> courseOfferingIds);

    List<ClassSchedule> findByTermInstanceIdAndStatusAndBatchIdIn(
        Long termInstanceId, ClassScheduleStatus status, List<Long> batchIds);

    boolean existsByTermInstanceId(Long termInstanceId);

    void deleteByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status);

    void deleteByTermInstanceId(Long termInstanceId);

    /**
     * Resolves each candidate row to a concrete (startTime, endTime) via its Period (THEORY) or
     * LabSlot (LAB), and compares actual time-range overlap rather than slot-id equality — the
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
          AND (
            (cs.sessionType = com.cms.model.enums.ClassSessionType.LAB
               AND cs.labSlot IS NOT NULL
               AND cs.labSlot.startTime < :endTime AND cs.labSlot.endTime > :startTime)
            OR
            (cs.sessionType = com.cms.model.enums.ClassSessionType.THEORY
               AND cs.period IS NOT NULL
               AND cs.period.startTime < :endTime AND cs.period.endTime > :startTime)
          )
        """)
    List<ClassSchedule> findOverlapping(@Param("dayOfWeek") DayOfWeek dayOfWeek,
                                         @Param("termInstanceId") Long termInstanceId,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("status") ClassScheduleStatus status,
                                         @Param("excludeId") Long excludeId);
}
