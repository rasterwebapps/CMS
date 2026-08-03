package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cms.model.HolidayTemplate;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;
import com.cms.service.HolidayTemplateDateCalculator.DateRange;

class HolidayTemplateDateCalculatorTest {

    private static HolidayTemplate yearly(int month, int dayOfMonth, int durationDays) {
        HolidayTemplate t = new HolidayTemplate();
        t.setRecurrenceType(HolidayRecurrenceType.YEARLY);
        t.setMonth(month);
        t.setDayOfMonth(dayOfMonth);
        t.setDurationDays(durationDays);
        return t;
    }

    private static HolidayTemplate monthly(WeekOfMonth weekOfMonth, DayOfWeek dayOfWeek, int durationDays) {
        HolidayTemplate t = new HolidayTemplate();
        t.setRecurrenceType(HolidayRecurrenceType.MONTHLY);
        t.setWeekOfMonth(weekOfMonth);
        t.setDayOfWeek(dayOfWeek);
        t.setDurationDays(durationDays);
        return t;
    }

    @Test
    void yearlyProducesOneOccurrenceWhenAcademicYearStaysWithinOneCalendarYear() {
        HolidayTemplate republicDay = yearly(1, 26, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            republicDay, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 1, 26), LocalDate.of(2026, 1, 26)));
    }

    @Test
    void yearlyProducesTwoOccurrencesWhenAcademicYearSpansTwoCalendarYears() {
        // AY runs Aug 2026 -> May 2027; Jan 26 falls in 2027 only.
        HolidayTemplate republicDay = yearly(1, 26, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            republicDay, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 5, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2027, 1, 26), LocalDate.of(2027, 1, 26)));
    }

    @Test
    void yearlySkipsFeb29InANonLeapYear() {
        HolidayTemplate feb29Holiday = yearly(2, 29, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            feb29Holiday, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(occurrences).isEmpty();
    }

    @Test
    void yearlyIncludesFeb29InALeapYear() {
        HolidayTemplate feb29Holiday = yearly(2, 29, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            feb29Holiday, LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 2, 29)));
    }

    @Test
    void yearlyExcludesAMultiDayHolidayThatWouldSpillPastTheAcademicYearEnd() {
        // 3-day holiday starting May 30 would end June 1, past a May 31 AY end -- excluded whole.
        HolidayTemplate springFestival = yearly(5, 30, 3);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            springFestival, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 5, 31));

        assertThat(occurrences).isEmpty();
    }

    @Test
    void monthlyProducesOneOccurrencePerMonthInRange() {
        HolidayTemplate secondSaturday = monthly(WeekOfMonth.SECOND, DayOfWeek.SATURDAY, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            secondSaturday, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 13), LocalDate.of(2026, 6, 13)),
            new DateRange(LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 11)),
            new DateRange(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 8)));
    }

    @Test
    void monthlyLastWeekdayUsesLastInMonth() {
        HolidayTemplate lastFriday = monthly(WeekOfMonth.LAST, DayOfWeek.FRIDAY, 1);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            lastFriday, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 26), LocalDate.of(2026, 6, 26)));
    }
}
