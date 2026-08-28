package com.cms.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cms.model.BlockedPeriod;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BlockedPeriodRepository;

/** Shared blocked-period predicate used by {@link TimetableStaffingService},
 *  {@link TimetableSkeletonService}, and {@link TimetableSwapService} — previously three separate
 *  copies of the same two-repository-call query that could drift out of sync. Matches by actual
 *  clock-time overlap against the candidate's {@code startTime}/{@code endTime} (mirroring {@link
 *  com.cms.repository.ClassScheduleRepository#findOverlapping}), not period-id equality — so a
 *  session placed in a Period row shaped differently from the blocked period's own row (e.g. a
 *  combined double-period spanning what BlockedPeriod knows only as "Period 4") still gets caught
 *  whenever its actual time range overlaps the block. Deliberately coarse on dates: a RECURRING
 *  block whose date range overlaps the term at all hard-blocks every occurrence of that day+time
 *  for the whole term (a weekly-template placement can't represent "blocked some weeks, not
 *  others"). Manually-created ONE_OFF blocks never reach this check — only RECURRING and
 *  holiday-auto-generated ONE_OFF blocks (scoped to {@code sourceCalendarEventId IS NOT NULL}) do. */
@Service
public class TimetableBlockedPeriodChecker {

    private final BlockedPeriodRepository blockedPeriodRepository;

    public TimetableBlockedPeriodChecker(BlockedPeriodRepository blockedPeriodRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    /** Returns the block's reason text, or empty if this day/time-range/term window is free.
     *  Saturday gets one extra, coarse gate on top of the usual BlockedPeriod checks: if {@code
     *  termInstance} has no working-Saturday pattern configured at all, every Saturday is blocked
     *  outright (Mon-Fri only, matching this term's default); once a pattern exists, the weekly
     *  slot is left open here (some Saturdays in the term will match it) and the exact date-level
     *  precision — which Saturdays actually produce a real class — is enforced downstream by
     *  {@code ClassScheduleOccurrenceService}, not here (this check has no specific calendar date
     *  to test against, only a recurring day-of-week). */
    public Optional<String> blockReason(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                         TermInstance termInstance) {
        String memoKey = dayOfWeek + "|" + startTime + "|" + endTime + "|" + termInstance.getId();
        return AutoScheduleRunCache.current()
            .map(cache -> cache.memoizedBlockReason(memoKey, () -> computeBlockReason(dayOfWeek, startTime, endTime, termInstance)))
            .orElseGet(() -> computeBlockReason(dayOfWeek, startTime, endTime, termInstance));
    }

    private Optional<String> computeBlockReason(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                                 TermInstance termInstance) {
        if (dayOfWeek == DayOfWeek.SATURDAY && termInstance.getWorkingSaturdayWeeks().isEmpty()) {
            return Optional.of("Saturday isn't enabled as a working day for this term yet — "
                + "configure a working-Saturday pattern first");
        }

        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, startTime, endTime, termInstance.getStartDate(), termInstance.getEndDate());
        if (!conflicts.isEmpty()) {
            return Optional.of(conflicts.get(0).getReason());
        }

        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        List<BlockedPeriod> holidayConflicts = blockedPeriodRepository.findHolidayOneOffBlocksInRange(
                startTime, endTime, termInstance.getStartDate(), termInstance.getEndDate())
            .stream()
            .filter(bp -> bp.getSpecificDate().getDayOfWeek() == targetDay)
            .toList();
        if (!holidayConflicts.isEmpty()) {
            return Optional.of(holidayConflicts.get(0).getReason());
        }
        return Optional.empty();
    }
}
