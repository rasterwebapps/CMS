package com.cms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cms.model.BlockedPeriod;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BlockedPeriodRepository;

/** Shared blocked-period predicate used by {@link TimetableStaffingService},
 *  {@link TimetableSkeletonService}, and {@link TimetableSwapService} — previously three separate
 *  copies of the same two-repository-call query that could drift out of sync. Deliberately coarse:
 *  a RECURRING block whose date range overlaps the term at all hard-blocks every occurrence of that
 *  day+period for the whole term (a weekly-template placement can't represent "blocked some weeks,
 *  not others"). Manually-created ONE_OFF blocks never reach this check — only RECURRING and
 *  holiday-auto-generated ONE_OFF blocks (scoped to {@code sourceCalendarEventId IS NOT NULL}) do. */
@Service
public class TimetableBlockedPeriodChecker {

    private final BlockedPeriodRepository blockedPeriodRepository;

    public TimetableBlockedPeriodChecker(BlockedPeriodRepository blockedPeriodRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    /** Returns the block's reason text, or empty if this day/period/term window is free. */
    public Optional<String> blockReason(DayOfWeek dayOfWeek, Long periodId, LocalDate termStart, LocalDate termEnd) {
        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, periodId, termStart, termEnd);
        if (!conflicts.isEmpty()) {
            return Optional.of(conflicts.get(0).getReason());
        }

        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        List<BlockedPeriod> holidayConflicts = blockedPeriodRepository.findHolidayOneOffBlocksInRange(
                periodId, termStart, termEnd)
            .stream()
            .filter(bp -> bp.getSpecificDate().getDayOfWeek() == targetDay)
            .toList();
        if (!holidayConflicts.isEmpty()) {
            return Optional.of(holidayConflicts.get(0).getReason());
        }
        return Optional.empty();
    }
}
