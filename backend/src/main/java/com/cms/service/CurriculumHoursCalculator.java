package com.cms.service;

import java.time.temporal.ChronoUnit;
import java.util.List;

import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;

/**
 * Shared hour/session-count math, extracted from {@link TimetableGenerationService} so the R3
 * Phase 4 skeleton builder can show the exact same "how many weekly sessions does this many
 * curriculum hours need" numbers the generator itself uses — one formula, not two copies that
 * could silently drift apart.
 */
public final class CurriculumHoursCalculator {

    private CurriculumHoursCalculator() {
    }

    /** Whole weeks spanned by the term, rounded up so a partial trailing week still counts as a
     *  full placement opportunity; at least 1 to avoid a divide-by-zero for a same-day term. */
    public static int weeksInTerm(TermInstance termInstance) {
        long days = ChronoUnit.DAYS.between(termInstance.getStartDate(), termInstance.getEndDate()) + 1;
        return (int) Math.max(1, Math.ceil(days / 7.0));
    }

    /** Total term hours are 60-minute CLOCK hours delivered by a recurring weekly SESSION whose
     *  own duration is {@code blockSizePeriods} consecutive periods (1 for THEORY, potentially
     *  more for a multi-period Lab/Clinical block — see {@link #resolveBlockSize}) of {@code
     *  slotDurationMinutes} each. Converts totalHours to minutes, divides by one full session's
     *  actual clock duration ({@code slotDurationMinutes * blockSizePeriods}, NOT a single period
     *  alone — a session is the real recurring unit being delivered, not one period of it) to get
     *  total sessions needed over the term, then spreads that across the term's weeks — rounded up
     *  at each step so the term never falls short. Fixed 2026-08-31 (OC-180 follow-up): this used
     *  to divide by {@code slotDurationMinutes} alone, silently assuming every session was exactly
     *  one period long — for a subject with a real multi-period block size, callers that then
     *  ALSO multiplied the (already too-high) result by that same block size were double-counting
     *  it, inflating both Capacity Auto-Plan's weekly-period-demand figures and the real
     *  auto-scheduler's required-sessions-per-week targets by roughly a factor of the block size. */
    public static int sessionsPerWeek(int totalHours, int weeksInTerm, double slotDurationMinutes, int blockSizePeriods) {
        if (totalHours <= 0) {
            return 0;
        }
        double sessionDurationMinutes = slotDurationMinutes * Math.max(1, blockSizePeriods);
        double sessionsNeededOverTerm = (totalHours * 60.0) / sessionDurationMinutes;
        return (int) Math.ceil(sessionsNeededOverTerm / weeksInTerm);
    }

    /** One representative duration for a pool of periods (a single duration, not exact per-slot
     *  minute accumulation) — correct today since every period in this system is configured
     *  uniformly, and a reasonable approximation if that ever changes. Falls back to 60 minutes
     *  for an empty pool, where the value is moot anyway since nothing will be placed. */
    public static double averageDurationMinutes(List<Integer> durationsMinutes) {
        return durationsMinutes.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(60.0);
    }

    /** How many consecutive periods one single session of this subject/sessionType must occupy
     *  ({@link Subject#getLabSessionBlockPeriods()}/{@link Subject#getClinicalSessionBlockPeriods()})
     *  — always 1 for THEORY, and defensively clamped to at least 1 for LAB/CLINICAL in case a
     *  subject's configured value is ever null/invalid. Shared by {@link
     *  TimetableGlobalAutoScheduleService} (per-session placement chunking) and {@link
     *  TimetableCapacityPlanningService} (weekly demand-period totals) so both agree on exactly
     *  the same block size for the same subject. */
    public static int resolveBlockSize(Subject subject, ClassSessionType sessionType) {
        if (subject == null) {
            return 1;
        }
        Integer configured = switch (sessionType) {
            case LAB -> subject.getLabSessionBlockPeriods();
            case CLINICAL -> subject.getClinicalSessionBlockPeriods();
            case THEORY -> 1;
            case LIBRARY -> throw new IllegalStateException(
                "Library has no curriculum Subject/block-size — its block size comes from the "
                    + "timetable.library_block_size_periods system configuration, not CurriculumHoursCalculator.");
        };
        return configured != null && configured >= 1 ? configured : 1;
    }
}
