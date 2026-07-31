package com.cms.service;

import java.time.temporal.ChronoUnit;
import java.util.List;

import com.cms.model.TermInstance;

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

    /** Total term hours are 60-minute CLOCK hours delivered by a recurring weekly slot whose own
     *  duration may not be 60 minutes — a 50-minute period needs more weekly occurrences than a
     *  60-minute one to deliver the same clock-hours. Converts totalHours to minutes, divides by
     *  the slot's actual duration to get total slots needed over the term, then spreads that
     *  across the term's weeks — rounded up at each step so the term never falls short. */
    public static int sessionsPerWeek(int totalHours, int weeksInTerm, double slotDurationMinutes) {
        if (totalHours <= 0) {
            return 0;
        }
        double slotsNeededOverTerm = (totalHours * 60.0) / slotDurationMinutes;
        return (int) Math.ceil(slotsNeededOverTerm / weeksInTerm);
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
}
