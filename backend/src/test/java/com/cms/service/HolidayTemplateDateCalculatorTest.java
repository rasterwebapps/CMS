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

    private static HolidayTemplate monthlyFixedDay(int dayOfMonth, int intervalCount, LocalDate anchorDate) {
        HolidayTemplate t = new HolidayTemplate();
        t.setRecurrenceType(HolidayRecurrenceType.MONTHLY);
        t.setDayOfMonth(dayOfMonth);
        t.setIntervalCount(intervalCount);
        t.setAnchorDate(anchorDate);
        t.setDurationDays(1);
        return t;
    }

    private static HolidayTemplate weekly(DayOfWeek dayOfWeek, int intervalCount, LocalDate anchorDate) {
        HolidayTemplate t = new HolidayTemplate();
        t.setRecurrenceType(HolidayRecurrenceType.WEEKLY);
        t.setDayOfWeek(dayOfWeek);
        t.setIntervalCount(intervalCount);
        t.setAnchorDate(anchorDate);
        t.setDurationDays(1);
        return t;
    }

    private static HolidayTemplate daily(int intervalCount, LocalDate anchorDate, LocalDate endDate) {
        HolidayTemplate t = new HolidayTemplate();
        t.setRecurrenceType(HolidayRecurrenceType.DAILY);
        t.setIntervalCount(intervalCount);
        t.setAnchorDate(anchorDate);
        t.setEndDate(endDate);
        t.setDurationDays(1);
        return t;
    }

    private static HolidayTemplate yearlyWithInterval(int month, int dayOfMonth, int intervalCount, LocalDate anchorDate) {
        HolidayTemplate t = yearly(month, dayOfMonth, 1);
        t.setIntervalCount(intervalCount);
        t.setAnchorDate(anchorDate);
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

    @Test
    void monthlyFixedDayProducesOneOccurrencePerMonth() {
        HolidayTemplate fifteenth = monthlyFixedDay(15, 1, null);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            fifteenth, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15)),
            new DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15)),
            new DateRange(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15)));
    }

    @Test
    void monthlyFixedDaySkipsMonthsThatDontHaveThatDay() {
        // June has 30 days; day 31 skips it entirely rather than rolling to July 1.
        HolidayTemplate thirtyFirst = monthlyFixedDay(31, 1, null);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            thirtyFirst, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31)),
            new DateRange(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 31)));
    }

    @Test
    void monthlyFixedDayRespectsIntervalAndAnchor() {
        // Every 2 months from a June 2026 anchor: Jun, Aug, Oct aligned -- Jul, Sep, Nov are not.
        HolidayTemplate everyOtherMonth = monthlyFixedDay(10, 2, LocalDate.of(2026, 6, 1));
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            everyOtherMonth, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10)),
            new DateRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)),
            new DateRange(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10)));
    }

    @Test
    void weeklyWithNoAnchorProducesEveryOccurrenceOfTheWeekday() {
        // June 1 2026 is a Monday.
        HolidayTemplate everyMonday = weekly(DayOfWeek.MONDAY, 1, null);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            everyMonday, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)),
            new DateRange(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 8)),
            new DateRange(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15)),
            new DateRange(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 22)),
            new DateRange(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 6, 29)));
    }

    @Test
    void weeklyWithIntervalPhaseLocksToTheAnchorsWeek() {
        // Every 2 weeks from a Jun 1 2026 (Monday) anchor: Jun 1, 15, 29 -- not Jun 8 or 22.
        HolidayTemplate everyOtherMonday = weekly(DayOfWeek.MONDAY, 2, LocalDate.of(2026, 6, 1));
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            everyOtherMonday, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)),
            new DateRange(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15)),
            new DateRange(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 6, 29)));
    }

    @Test
    void dailyRepeatsEveryIntervalDaysFromAnchor() {
        HolidayTemplate everyThirdDay = daily(3, LocalDate.of(2026, 6, 1), null);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            everyThirdDay, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)),
            new DateRange(LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 4)),
            new DateRange(LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 7)),
            new DateRange(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10)));
    }

    @Test
    void dailyAlignsToAnchorEvenWhenAnchorIsBeforeTheWindow() {
        HolidayTemplate everyThirdDay = daily(3, LocalDate.of(2026, 5, 30), null);
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            everyThirdDay, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

        assertThat(occurrences).containsExactly(
            new DateRange(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 2)),
            new DateRange(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 5)),
            new DateRange(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 8)));
    }

    @Test
    void endDateCutsGenerationShortEvenWhenTheAcademicYearContinuesFurther() {
        HolidayTemplate dailyTemplate = daily(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            dailyTemplate, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).hasSize(5);
        assertThat(occurrences.get(4).start()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void returnsEmptyWhenEndDateIsBeforeTheWindowEntirely() {
        HolidayTemplate dailyTemplate = daily(1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
            dailyTemplate, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).isEmpty();
    }

    @Test
    void yearlyIntervalSkipsNonAlignedAcademicYears() {
        // Every 2 years from a 2026 anchor -- the AY containing Jan 2026 gets an occurrence...
        HolidayTemplate everyOtherYear = yearlyWithInterval(1, 26, 2, LocalDate.of(2026, 1, 1));
        List<DateRange> onYear = HolidayTemplateDateCalculator.computeOccurrences(
            everyOtherYear, LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 31));
        assertThat(onYear).containsExactly(
            new DateRange(LocalDate.of(2026, 1, 26), LocalDate.of(2026, 1, 26)));

        // ...but the very next AY (which would otherwise contain Jan 2027) does not, since 2027
        // isn't aligned with the every-2-years cadence from the 2026 anchor.
        List<DateRange> offYear = HolidayTemplateDateCalculator.computeOccurrences(
            everyOtherYear, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 5, 31));
        assertThat(offYear).isEmpty();
    }
}
