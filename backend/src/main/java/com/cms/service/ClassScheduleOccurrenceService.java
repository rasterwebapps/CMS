package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.CalendarEventType;
import com.cms.repository.CalendarEventRepository;

/**
 * Resolves the real calendar dates a recurring {@link ClassSchedule} row actually fires on within
 * a window — the piece {@link PersonalTimetableService}'s class javadoc notes doesn't exist yet.
 * Pure computation over {@link ClassSchedule#getDayOfWeek()} + {@link TermInstance} bounds, minus
 * HOLIDAY {@link CalendarEvent} dates; never writes anything, never leaks into generation (which
 * stays a weekly-recurring placement problem per {@link TimetableGenerationService}'s own javadoc).
 */
@Service
@Transactional(readOnly = true)
public class ClassScheduleOccurrenceService {

    private final CalendarEventRepository calendarEventRepository;

    public ClassScheduleOccurrenceService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    /** Occurrence dates for one schedule, clamped to both the term's own bounds and the
     *  caller-supplied [from, to] window, skipping any date covered by a HOLIDAY event. */
    public List<LocalDate> occurrenceDatesFor(ClassSchedule schedule, LocalDate from, LocalDate to) {
        TermInstance term = schedule.getTermInstance();
        LocalDate start = maxDate(from, term.getStartDate());
        LocalDate end = minDate(to, term.getEndDate());
        if (start.isAfter(end)) {
            return List.of();
        }
        List<CalendarEvent> holidays = calendarEventRepository.findOverlapping(
            term.getAcademicYear().getId(), CalendarEventType.HOLIDAY, start, end);
        return datesFor(schedule, start, end, holidays);
    }

    /** Batched variant for calendar/grid rendering — fetches holidays once per distinct academic
     *  year across the whole batch instead of once per schedule, then reuses that same cache for
     *  every schedule's clamped range (a superset fetch is fine; we're never looking outside
     *  [from, to] regardless of an individual schedule's own term bounds). */
    public Map<Long, List<LocalDate>> occurrenceDatesForSchedules(
            List<ClassSchedule> schedules, LocalDate from, LocalDate to) {
        Map<Long, List<CalendarEvent>> holidaysByAcademicYear = new HashMap<>();
        Map<Long, List<LocalDate>> result = new HashMap<>();

        for (ClassSchedule schedule : schedules) {
            TermInstance term = schedule.getTermInstance();
            LocalDate start = maxDate(from, term.getStartDate());
            LocalDate end = minDate(to, term.getEndDate());
            if (start.isAfter(end)) {
                result.put(schedule.getId(), List.of());
                continue;
            }
            Long academicYearId = term.getAcademicYear().getId();
            List<CalendarEvent> holidays = holidaysByAcademicYear.computeIfAbsent(academicYearId,
                id -> calendarEventRepository.findOverlapping(id, CalendarEventType.HOLIDAY, from, to));
            result.put(schedule.getId(), datesFor(schedule, start, end, holidays));
        }
        return result;
    }

    private static List<LocalDate> datesFor(ClassSchedule schedule, LocalDate start, LocalDate end,
                                             List<CalendarEvent> holidays) {
        // com.cms.model.enums.DayOfWeek only has MONDAY..SATURDAY (no SUNDAY) -- names line up
        // exactly with java.time.DayOfWeek's, so valueOf() is a safe direct mapping.
        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(schedule.getDayOfWeek().name());
        LocalDate first = start.with(TemporalAdjusters.nextOrSame(targetDay));

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(end); date = date.plusWeeks(1)) {
            if (isHoliday(date, holidays)) {
                continue;
            }
            dates.add(date);
        }
        return dates;
    }

    private static boolean isHoliday(LocalDate date, List<CalendarEvent> holidays) {
        for (CalendarEvent holiday : holidays) {
            if (!date.isBefore(holiday.getStartDate()) && !date.isAfter(holiday.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
