package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CalendarEvent;
import com.cms.model.enums.CalendarEventType;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByAcademicYearIdOrderByStartDate(Long academicYearId);

    List<CalendarEvent> findBySemesterIdOrderByStartDate(Long semesterId);

    List<CalendarEvent> findByAcademicYearIdAndEventTypeOrderByStartDate(
        Long academicYearId, CalendarEventType eventType);
}
