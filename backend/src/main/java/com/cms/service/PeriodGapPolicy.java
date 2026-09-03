package com.cms.service;

import java.time.Duration;
import java.util.List;

import com.cms.model.Period;
import com.cms.model.enums.ClassSessionType;

/**
 * Classifies the clock-time gap between two ADJACENT-BY-POSITION active {@link Period}s as either
 * the day's one lunch break or an ordinary recess — derived purely from the data (the single
 * longest gap between any two consecutive active periods in the day is lunch; every shorter
 * nonzero gap is a recess), never a hardcoded period id, so this stays correct if periods/timings
 * are ever reconfigured.
 *
 * <p>Real institutional rule (per the college): a CLINICAL posting doesn't pause for a short
 * college recess the way a THEORY/LAB session does — a half-day (forenoon or afternoon) clinical
 * block runs straight through it. It still never crosses the actual lunch break, since that's a
 * real meal/duty-change boundary, not just a bell gap between two teaching periods. THEORY and LAB
 * keep requiring a strictly zero-gap consecutive run either way — this exception is CLINICAL-only.
 *
 * <p>This deliberately narrows part of the "spanned periods must be back-to-back with no break in
 * between" rule {@link TimetableSkeletonService#resolveSpanPeriods} and {@code
 * TimetableGlobalAutoScheduleService#tryPlaceAndStaff} both enforce — that rule stays exactly as
 * strict as before for every case except a CLINICAL block spanning a real recess (not lunch).
 *
 * <p><b>How scarce this makes big blocks — worth knowing before touching any placement code.</b>
 * The rule here is what decides how many legal positions a multi-period session actually has, and
 * the answer is far smaller than the period count suggests. On the live 8-period day (P1–P4, lunch,
 * P5–P8) a 4-period CLINICAL block fits in exactly TWO places — the forenoon run and the afternoon
 * run — because any other start index would straddle lunch. Six days therefore offer roughly a
 * dozen Clinical windows for the entire week, and a single one-period session placed at P4 or P5
 * annihilates a whole window rather than costing one period. That asymmetry is why {@code
 * TimetableGlobalAutoScheduleService} places LAB/CLINICAL blocks before THEORY and before its
 * Library/Self-Study filler, and why it rebuilds the draft grid instead of adding to it — see that
 * class's "Placement order" javadoc section. Widening the lunch break, shortening a period, or
 * adding a recess changes this arithmetic directly.
 */
final class PeriodGapPolicy {

    private PeriodGapPolicy() {
    }

    /** True if the clock-time gap between {@code before} and {@code after} (already established
     *  by the caller to be adjacent by position in the active-period list) may be spanned by one
     *  {@code sessionType} session despite being nonzero. Only ever true for CLINICAL, and only for
     *  a gap strictly shorter than the day's single longest gap (the lunch break) — a day with only
     *  one gap at all has nothing to compare against, so that gap is conservatively treated as
     *  lunch (never crossable). */
    static boolean gapCrossableFor(ClassSessionType sessionType, Period before, Period after,
                                    List<Period> activeOrderedPeriods) {
        if (sessionType != ClassSessionType.CLINICAL) {
            return false;
        }
        Duration thisGap = Duration.between(before.getEndTime(), after.getStartTime());
        if (thisGap.isZero() || thisGap.isNegative()) {
            return true;
        }
        return thisGap.compareTo(longestGap(activeOrderedPeriods)) < 0;
    }

    private static Duration longestGap(List<Period> activeOrderedPeriods) {
        Duration longest = Duration.ZERO;
        for (int i = 1; i < activeOrderedPeriods.size(); i++) {
            Duration gap = Duration.between(activeOrderedPeriods.get(i - 1).getEndTime(), activeOrderedPeriods.get(i).getStartTime());
            if (gap.compareTo(longest) > 0) {
                longest = gap;
            }
        }
        return longest;
    }
}
