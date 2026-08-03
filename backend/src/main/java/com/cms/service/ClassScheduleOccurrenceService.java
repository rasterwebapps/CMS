package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.repository.BlockedPeriodRepository;

/**
 * Resolves the real calendar dates a recurring {@link ClassSchedule} row actually fires on within
 * a window — the piece {@link PersonalTimetableService}'s class javadoc notes doesn't exist yet.
 * Pure computation over {@link ClassSchedule#getDayOfWeek()} + {@link TermInstance} bounds, minus
 * any date on which {@link ClassSchedule#getPeriod()} is covered by a {@link BlockedPeriod} row --
 * holiday-derived (see {@code BlockedPeriod.sourceCalendarEvent}) or manually created alike, since
 * either way no class actually happens then. This is deliberately period-precise rather than the
 * old whole-day HOLIDAY-event check it replaces: a half-day holiday only cancels the periods it
 * actually blocked, and deleting a holiday-derived block for one period on one date (the "unblock
 * for a special class" override) is what makes that session occur again -- no separate flag needed.
 * Never writes anything, never leaks into generation (which stays a weekly-recurring placement
 * problem per {@link TimetableGenerationService}'s own javadoc).
 */
@Service
@Transactional(readOnly = true)
public class ClassScheduleOccurrenceService {

    private final BlockedPeriodRepository blockedPeriodRepository;

    public ClassScheduleOccurrenceService(BlockedPeriodRepository blockedPeriodRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    /** Occurrence dates for one schedule, clamped to both the term's own bounds and the
     *  caller-supplied [from, to] window, skipping any date on which this schedule's period is
     *  blocked. */
    public List<LocalDate> occurrenceDatesFor(ClassSchedule schedule, LocalDate from, LocalDate to) {
        TermInstance term = schedule.getTermInstance();
        LocalDate start = maxDate(from, term.getStartDate());
        LocalDate end = minDate(to, term.getEndDate());
        if (start.isAfter(end)) {
            return List.of();
        }
        List<BlockedPeriod> blocks = blockedPeriodRepository.findApplicableForPeriodInRange(
            schedule.getPeriod().getId(), start, end);
        return datesFor(schedule, start, end, blocks);
    }

    /** Batched variant for calendar/grid rendering — fetches each distinct period's blocks once
     *  across the whole batch instead of once per schedule, then reuses that cache for every
     *  schedule sharing the same period (a superset fetch over [from, to] is fine regardless of an
     *  individual schedule's own term bounds). */
    public Map<Long, List<LocalDate>> occurrenceDatesForSchedules(
            List<ClassSchedule> schedules, LocalDate from, LocalDate to) {
        Map<Long, List<BlockedPeriod>> blocksByPeriod = new HashMap<>();
        Map<Long, List<LocalDate>> result = new HashMap<>();

        for (ClassSchedule schedule : schedules) {
            TermInstance term = schedule.getTermInstance();
            LocalDate start = maxDate(from, term.getStartDate());
            LocalDate end = minDate(to, term.getEndDate());
            if (start.isAfter(end)) {
                result.put(schedule.getId(), List.of());
                continue;
            }
            Long periodId = schedule.getPeriod().getId();
            List<BlockedPeriod> blocks = blocksByPeriod.computeIfAbsent(periodId,
                id -> blockedPeriodRepository.findApplicableForPeriodInRange(id, from, to));
            result.put(schedule.getId(), datesFor(schedule, start, end, blocks));
        }
        return result;
    }

    private static List<LocalDate> datesFor(ClassSchedule schedule, LocalDate start, LocalDate end,
                                             List<BlockedPeriod> blocks) {
        // com.cms.model.enums.DayOfWeek only has MONDAY..SATURDAY (no SUNDAY) -- names line up
        // exactly with java.time.DayOfWeek's, so valueOf() is a safe direct mapping.
        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(schedule.getDayOfWeek().name());
        LocalDate first = start.with(TemporalAdjusters.nextOrSame(targetDay));

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(end); date = date.plusWeeks(1)) {
            if (isBlocked(date, blocks)) {
                continue;
            }
            dates.add(date);
        }
        return dates;
    }

    /** Mirrors the exact ONE_OFF/RECURRING matching logic already used by
     *  {@code TimetableCapacityPlanningService.blockedHoursInTerm} -- kept consistent so both
     *  agree on which dates are actually non-teaching. */
    private static boolean isBlocked(LocalDate date, List<BlockedPeriod> blocks) {
        com.cms.model.enums.DayOfWeek appDayOfWeek = com.cms.model.enums.DayOfWeek.valueOf(date.getDayOfWeek().name());
        for (BlockedPeriod block : blocks) {
            boolean matches = block.getBlockType() == BlockType.ONE_OFF
                ? date.equals(block.getSpecificDate())
                : appDayOfWeek == block.getDayOfWeek()
                    && !date.isBefore(block.getRangeStartDate()) && !date.isAfter(block.getRangeEndDate());
            if (matches) {
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
