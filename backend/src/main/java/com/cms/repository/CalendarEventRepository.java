package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CalendarEvent;
import com.cms.model.enums.CalendarEventType;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByAcademicYearIdOrderByStartDate(Long academicYearId);

    List<CalendarEvent> findByAcademicYearIdAndEventTypeOrderByStartDate(
        Long academicYearId, CalendarEventType eventType);

    /** Display-time-only holiday lookup for the personal/browse timetable views — generation
     *  itself never calls this, since the schedule stays a recurring weekly template (no
     *  calendar dates baked into placement, per the agreed v1 scope). */
    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.academicYear.id = :academicYearId " +
           "AND ce.eventType = :eventType AND ce.startDate <= :rangeEnd AND ce.endDate >= :rangeStart")
    List<CalendarEvent> findOverlapping(@Param("academicYearId") Long academicYearId,
                                         @Param("eventType") CalendarEventType eventType,
                                         @Param("rangeStart") LocalDate rangeStart,
                                         @Param("rangeEnd") LocalDate rangeEnd);

    /** HOLIDAY and EXAM days both take a full day off regular teaching -- used by the Capacity
     *  Planner's working-days-in-term calculation. Other event types (CULTURAL/SPORTS/WORKSHOP)
     *  are typically partial-day and deliberately excluded here. */
    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.academicYear.id = :academicYearId " +
           "AND ce.eventType IN (com.cms.model.enums.CalendarEventType.HOLIDAY, com.cms.model.enums.CalendarEventType.EXAM) " +
           "AND ce.startDate <= :rangeEnd AND ce.endDate >= :rangeStart")
    List<CalendarEvent> findNonTeachingDaysOverlapping(@Param("academicYearId") Long academicYearId,
                                                         @Param("rangeStart") LocalDate rangeStart,
                                                         @Param("rangeEnd") LocalDate rangeEnd);

    /** Every event (any type) overlapping a proposed date range, used by the flyout's
     *  save-time conflict check. Unlike {@link #findOverlapping}, not filtered to one eventType --
     *  an admin should know about an Exam already scheduled during a new Holiday, not just other
     *  Holidays. {@code excludeId} lets an in-progress edit ignore its own row. */
    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.academicYear.id = :academicYearId " +
           "AND (:excludeId IS NULL OR ce.id <> :excludeId) " +
           "AND ce.startDate <= :rangeEnd AND ce.endDate >= :rangeStart")
    List<CalendarEvent> findOverlappingAnyType(@Param("academicYearId") Long academicYearId,
                                                @Param("rangeStart") LocalDate rangeStart,
                                                @Param("rangeEnd") LocalDate rangeEnd,
                                                @Param("excludeId") Long excludeId);

    /** Future-dated events seeded from one HolidayTemplate -- used by the "delete this and all
     *  future occurrences" series-delete (past instances are never touched). */
    List<CalendarEvent> findBySourceHolidayTemplateIdAndStartDateGreaterThanEqual(
        Long sourceHolidayTemplateId, LocalDate cutoff);
}
