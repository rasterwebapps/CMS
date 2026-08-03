package com.cms.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cms.model.HolidayTemplate;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

/** Pure, stateless computation of concrete date ranges a {@link HolidayTemplate} produces within
 *  one AcademicYear's bounds -- used by {@code HolidayTemplateSeedingService} when a new
 *  AcademicYear is created, and by {@code CalendarEventService} when a repeating event is created
 *  inline from the Add Event form. Never touches the DB; every method here is a plain function of
 *  its arguments, kept separate from the seeding service so it's trivially unit-testable.
 *
 *  Mirrors a simplified iOS/Google Calendar Repeat rule: a frequency (recurrenceType) + interval
 *  ("every N units") + frequency-specific pattern fields + an optional end date. {@code anchorDate}
 *  is the reference occurrence interval-alignment counts from -- required whenever intervalCount
 *  > 1 (enforced by {@code HolidayTemplateService.validateShape}), optional at intervalCount == 1
 *  where the pattern fields alone already determine every occurrence. */
final class HolidayTemplateDateCalculator {

    private HolidayTemplateDateCalculator() {
    }

    record DateRange(LocalDate start, LocalDate end) {
    }

    static List<DateRange> computeOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        LocalDate effectiveEnd = template.getEndDate() != null && template.getEndDate().isBefore(ayEnd)
            ? template.getEndDate() : ayEnd;
        if (effectiveEnd.isBefore(ayStart)) {
            return List.of();
        }
        return switch (template.getRecurrenceType()) {
            case YEARLY -> yearlyOccurrences(template, ayStart, effectiveEnd);
            case MONTHLY -> monthlyOccurrences(template, ayStart, effectiveEnd);
            case WEEKLY -> weeklyOccurrences(template, ayStart, effectiveEnd);
            case DAILY -> dailyOccurrences(template, ayStart, effectiveEnd);
        };
    }

    /** One candidate date per calendar year the window touches (usually 1, up to 2 if the window
     *  spans two calendar years, e.g. an AcademicYear running Aug-May), restricted to years
     *  aligned with intervalCount from anchorDate's year (always aligned if no anchor is set,
     *  which is only valid at intervalCount == 1). Feb 29 in a non-leap year is simply skipped
     *  that year -- no roll-forward/back policy. Only included if the full
     *  [start, start+durationDays-1] span fits inside the window. */
    private static List<DateRange> yearlyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        Integer anchorYear = template.getAnchorDate() != null ? template.getAnchorDate().getYear() : null;
        Set<Integer> candidateYears = new LinkedHashSet<>(List.of(ayStart.getYear(), ayEnd.getYear()));
        for (int year : candidateYears) {
            if (anchorYear != null && !aligned(year - anchorYear, template.getIntervalCount())) {
                continue;
            }
            LocalDate start;
            try {
                start = LocalDate.of(year, template.getMonth(), template.getDayOfMonth());
            } catch (java.time.DateTimeException invalidForThisYear) {
                continue;
            }
            addIfWithin(results, start, template.getDurationDays(), ayStart, ayEnd);
        }
        return results;
    }

    /** One candidate date per calendar month the window touches, restricted to months aligned
     *  with intervalCount from anchorDate's month. Two mutually exclusive sub-patterns: a fixed
     *  day-of-month (dayOfMonth set), or an nth-weekday-of-month (weekOfMonth+dayOfWeek set, via
     *  TemporalAdjusters). Same full-containment rule as yearly. */
    private static List<DateRange> monthlyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        YearMonth anchorMonth = template.getAnchorDate() != null ? YearMonth.from(template.getAnchorDate()) : null;
        boolean fixedDayOfMonth = template.getDayOfMonth() != null;
        java.time.DayOfWeek javaDow = fixedDayOfMonth ? null : java.time.DayOfWeek.valueOf(template.getDayOfWeek().name());

        YearMonth cursor = YearMonth.from(ayStart);
        YearMonth last = YearMonth.from(ayEnd);
        while (!cursor.isAfter(last)) {
            boolean isAligned = anchorMonth == null
                || aligned(ChronoUnit.MONTHS.between(anchorMonth, cursor), template.getIntervalCount());
            if (isAligned) {
                LocalDate start;
                if (fixedDayOfMonth) {
                    int lastDayOfThisMonth = cursor.lengthOfMonth();
                    if (template.getDayOfMonth() > lastDayOfThisMonth) {
                        cursor = cursor.plusMonths(1);
                        continue; // e.g. the 30th in February -- skip this month, no roll-forward
                    }
                    start = cursor.atDay(template.getDayOfMonth());
                } else {
                    LocalDate firstOfMonth = cursor.atDay(1);
                    start = template.getWeekOfMonth() == WeekOfMonth.LAST
                        ? firstOfMonth.with(TemporalAdjusters.lastInMonth(javaDow))
                        : firstOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(ordinal(template.getWeekOfMonth()), javaDow));
                }
                addIfWithin(results, start, template.getDurationDays(), ayStart, ayEnd);
            }
            cursor = cursor.plusMonths(1);
        }
        return results;
    }

    /** Every intervalCount weeks on dayOfWeek. When anchorDate is set, occurrences are phase-locked
     *  to the anchor's own week (the first anchorDate-or-later occurrence of dayOfWeek); otherwise
     *  (only valid at intervalCount == 1) every occurrence of dayOfWeek in the window counts. */
    private static List<DateRange> weeklyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        java.time.DayOfWeek javaDow = java.time.DayOfWeek.valueOf(template.getDayOfWeek().name());
        int stepDays = template.getIntervalCount() * 7;

        LocalDate first;
        if (template.getAnchorDate() != null) {
            LocalDate anchorOccurrence = template.getAnchorDate().with(TemporalAdjusters.nextOrSame(javaDow));
            long daysFromAnchor = ChronoUnit.DAYS.between(anchorOccurrence, ayStart);
            long steps = daysFromAnchor <= 0 ? 0 : ceilDiv(daysFromAnchor, stepDays);
            first = anchorOccurrence.plusDays(steps * stepDays);
        } else {
            first = ayStart.with(TemporalAdjusters.nextOrSame(javaDow));
        }

        for (LocalDate date = first; !date.isAfter(ayEnd); date = date.plusDays(stepDays)) {
            addIfWithin(results, date, template.getDurationDays(), ayStart, ayEnd);
        }
        return results;
    }

    /** Every intervalCount days from anchorDate (required for DAILY). */
    private static List<DateRange> dailyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        LocalDate anchor = template.getAnchorDate();
        int step = template.getIntervalCount();

        long daysFromAnchor = ChronoUnit.DAYS.between(anchor, ayStart);
        long steps = daysFromAnchor <= 0 ? 0 : ceilDiv(daysFromAnchor, step);
        LocalDate first = anchor.plusDays(steps * step);

        for (LocalDate date = first; !date.isAfter(ayEnd); date = date.plusDays(step)) {
            addIfWithin(results, date, template.getDurationDays(), ayStart, ayEnd);
        }
        return results;
    }

    private static void addIfWithin(List<DateRange> results, LocalDate start, int durationDays,
                                     LocalDate windowStart, LocalDate windowEnd) {
        LocalDate end = start.plusDays(durationDays - 1);
        if (!start.isBefore(windowStart) && !end.isAfter(windowEnd)) {
            results.add(new DateRange(start, end));
        }
    }

    private static boolean aligned(long unitsSinceAnchor, int intervalCount) {
        return unitsSinceAnchor >= 0 && unitsSinceAnchor % intervalCount == 0;
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static int ordinal(WeekOfMonth weekOfMonth) {
        return switch (weekOfMonth) {
            case FIRST -> 1;
            case SECOND -> 2;
            case THIRD -> 3;
            case FOURTH -> 4;
            case LAST -> throw new IllegalStateException("LAST is handled via lastInMonth, not an ordinal");
        };
    }
}
