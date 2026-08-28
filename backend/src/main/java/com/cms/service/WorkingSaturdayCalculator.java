package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.cms.model.TermInstance;
import com.cms.model.enums.WeekOfMonth;

/** Pure, stateless helper deciding whether a given calendar Saturday counts as a real working day
 *  under a {@link TermInstance}'s opt-in {@code workingSaturdayWeeks} pattern (e.g. "only the 1st
 *  Saturday of every month"). An empty pattern means the term hasn't opted in at all — every
 *  Saturday is a non-working day, matching {@link TimetableBlockedPeriodChecker}'s placement-time
 *  gate. Shared by {@link ClassScheduleOccurrenceService} (which Saturdays actually produce a real
 *  class instance) and the hours-aggregation service (how much of a Saturday-placed weekly session
 *  actually counts toward scheduled hours). */
final class WorkingSaturdayCalculator {

    private WorkingSaturdayCalculator() {
    }

    /** True if {@code date} is a Saturday that is NOT a designated working day under {@code term}'s
     *  pattern — i.e. it should never produce a real class occurrence. False for every non-Saturday
     *  date (nothing to suppress) and for a Saturday that does match the pattern. */
    static boolean isNonWorkingSaturday(LocalDate date, TermInstance term) {
        if (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            return false;
        }
        Set<WeekOfMonth> allowed = term.getWorkingSaturdayWeeks();
        if (allowed.isEmpty()) {
            return true;
        }
        return !matches(date, allowed);
    }

    private static boolean matches(LocalDate date, Set<WeekOfMonth> allowed) {
        int ordinal = ((date.getDayOfMonth() - 1) / 7) + 1; // 1..5
        boolean isLastOccurrenceInMonth = date.getDayOfMonth() + 7 > date.lengthOfMonth();
        if (isLastOccurrenceInMonth && allowed.contains(WeekOfMonth.LAST)) {
            return true;
        }
        WeekOfMonth ordinalWeek = switch (ordinal) {
            case 1 -> WeekOfMonth.FIRST;
            case 2 -> WeekOfMonth.SECOND;
            case 3 -> WeekOfMonth.THIRD;
            case 4 -> WeekOfMonth.FOURTH;
            default -> null; // a rare 5th Saturday only ever matches via LAST, above
        };
        return ordinalWeek != null && allowed.contains(ordinalWeek);
    }

    /** How many of {@code date}'s Saturdays within [termStart, termEnd] are real working days
     *  under {@code term}'s pattern — used to compute a Saturday-placed weekly session's honest
     *  contribution to scheduled hours (a "1st Saturday only" session fires far less often than a
     *  real weekly Mon-Fri one). Returns the term's total Saturday count unfiltered when no
     *  pattern is configured — callers that reach here already know Saturday placement is blocked
     *  in that case (see {@link TimetableBlockedPeriodChecker}), so this is only ever asked about
     *  actually-placed Saturday sessions, which can't exist without a pattern already configured. */
    static long workingSaturdayCount(TermInstance term) {
        LocalDate start = term.getStartDate();
        LocalDate end = term.getEndDate();
        LocalDate firstSaturday = start.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY));
        long totalSaturdays = firstSaturday.isAfter(end) ? 0
            : ChronoUnit.WEEKS.between(firstSaturday, end) + 1;
        if (term.getWorkingSaturdayWeeks().isEmpty()) {
            return totalSaturdays;
        }
        long count = 0;
        for (LocalDate date = firstSaturday; !date.isAfter(end); date = date.plusWeeks(1)) {
            if (!isNonWorkingSaturday(date, term)) {
                count++;
            }
        }
        return count;
    }
}
