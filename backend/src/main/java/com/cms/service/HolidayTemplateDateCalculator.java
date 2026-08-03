package com.cms.service;

import java.time.LocalDate;
import java.time.YearMonth;
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
 *  AcademicYear is created. Never touches the DB; every method here is a plain function of its
 *  arguments, kept separate from the seeding service so it's trivially unit-testable. */
final class HolidayTemplateDateCalculator {

    private HolidayTemplateDateCalculator() {
    }

    record DateRange(LocalDate start, LocalDate end) {
    }

    static List<DateRange> computeOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        if (template.getRecurrenceType() == HolidayRecurrenceType.YEARLY) {
            return yearlyOccurrences(template, ayStart, ayEnd);
        }
        return monthlyOccurrences(template, ayStart, ayEnd);
    }

    /** One candidate date per calendar year the AcademicYear's range touches (usually 1, up to 2
     *  if the year spans two calendar years, e.g. Aug-May). Feb 29 in a non-leap year is simply
     *  skipped that year -- no roll-forward/back policy. Only included if the full
     *  [start, start+durationDays-1] span fits inside the AcademicYear's own dates. */
    private static List<DateRange> yearlyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        Set<Integer> candidateYears = new LinkedHashSet<>(List.of(ayStart.getYear(), ayEnd.getYear()));
        for (int year : candidateYears) {
            LocalDate start;
            try {
                start = LocalDate.of(year, template.getMonth(), template.getDayOfMonth());
            } catch (java.time.DateTimeException invalidForThisYear) {
                continue;
            }
            LocalDate end = start.plusDays(template.getDurationDays() - 1);
            if (!start.isBefore(ayStart) && !end.isAfter(ayEnd)) {
                results.add(new DateRange(start, end));
            }
        }
        return results;
    }

    /** One candidate date per calendar month the AcademicYear's range touches, via
     *  TemporalAdjusters.dayOfWeekInMonth(n, dow) (or lastInMonth for LAST). Same full-containment
     *  rule as yearly -- a holiday clipped at either edge of the AcademicYear is skipped entirely
     *  rather than partially generated. */
    private static List<DateRange> monthlyOccurrences(HolidayTemplate template, LocalDate ayStart, LocalDate ayEnd) {
        List<DateRange> results = new ArrayList<>();
        java.time.DayOfWeek javaDow = java.time.DayOfWeek.valueOf(template.getDayOfWeek().name());

        YearMonth cursor = YearMonth.from(ayStart);
        YearMonth last = YearMonth.from(ayEnd);
        while (!cursor.isAfter(last)) {
            LocalDate firstOfMonth = cursor.atDay(1);
            LocalDate start = template.getWeekOfMonth() == WeekOfMonth.LAST
                ? firstOfMonth.with(TemporalAdjusters.lastInMonth(javaDow))
                : firstOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(ordinal(template.getWeekOfMonth()), javaDow));
            LocalDate end = start.plusDays(template.getDurationDays() - 1);
            if (!start.isBefore(ayStart) && !end.isAfter(ayEnd)) {
                results.add(new DateRange(start, end));
            }
            cursor = cursor.plusMonths(1);
        }
        return results;
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
