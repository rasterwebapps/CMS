package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cms.model.BlockedPeriod;
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

    /** Returns the block's reason text, or empty if this day/time-range/term window is free. */
    public Optional<String> blockReason(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                         LocalDate termStart, LocalDate termEnd) {
        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, startTime, endTime, termStart, termEnd);
        if (!conflicts.isEmpty()) {
            return Optional.of(conflicts.get(0).getReason());
        }

        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        List<BlockedPeriod> holidayConflicts = blockedPeriodRepository.findHolidayOneOffBlocksInRange(
                startTime, endTime, termStart, termEnd)
            .stream()
            .filter(bp -> bp.getSpecificDate().getDayOfWeek() == targetDay)
            .toList();
        if (!holidayConflicts.isEmpty()) {
            return Optional.of(holidayConflicts.get(0).getReason());
        }
        return Optional.empty();
    }
}
