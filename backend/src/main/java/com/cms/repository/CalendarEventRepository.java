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
}
