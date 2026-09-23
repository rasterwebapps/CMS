package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cms.model.TermInstance;
import com.cms.model.enums.WeekOfMonth;

/** Locks in the arithmetic behind planning against the term's total hours: a partial week-of-month
 *  pattern is nothing like "every Saturday", and {@link WorkingSaturdayCalculator#runsInTerm} credits
 *  a Saturday session with exactly this count of runs. */
class WorkingSaturdayCalculatorTest {

    /** The real SKSCON 2026-2027 ODD term the Saturday-capacity issue was reported against. */
    private static TermInstance oddTerm(Set<WeekOfMonth> weeks) {
        TermInstance term = new TermInstance();
        term.setStartDate(LocalDate.of(2026, 10, 1));
        term.setEndDate(LocalDate.of(2027, 3, 31));
        term.setWorkingSaturdayWeeks(weeks);
        return term;
    }

    @Test
    void firstSaturdayOnlyYieldsOneDayPerMonthNotEverySaturday() {
        // The exact misreading the UI count now prevents: ticking "1st Saturday" reads as "Saturdays
        // are on", but delivers 6 working days across a 6-month term, not 26.
        assertThat(WorkingSaturdayCalculator.workingSaturdayCount(oddTerm(EnumSet.of(WeekOfMonth.FIRST)))).isEqualTo(6);
    }

    @Test
    void allFiveWeekPatternsCoverEverySaturdayInTheTerm() {
        // FIRST..FOURTH plus LAST covers a 5th Saturday too, so all 26 Saturdays are working days.
        assertThat(WorkingSaturdayCalculator.workingSaturdayCount(oddTerm(EnumSet.allOf(WeekOfMonth.class)))).isEqualTo(26);
    }

    @Test
    void runsInTermCreditsAWeekdayEveryWeekAndASaturdayOnlyItsWorkingSaturdays() {
        // The user's own arithmetic: 26 weeks x 5 weekdays, plus 6 first Saturdays.
        TermInstance firstOnly = oddTerm(EnumSet.of(WeekOfMonth.FIRST));
        assertThat(WorkingSaturdayCalculator.runsInTerm(com.cms.model.enums.DayOfWeek.TUESDAY, firstOnly, 26)).isEqualTo(26);
        assertThat(WorkingSaturdayCalculator.runsInTerm(com.cms.model.enums.DayOfWeek.SATURDAY, firstOnly, 26)).isEqualTo(6);
        assertThat(WorkingSaturdayCalculator.runsInTerm(com.cms.model.enums.DayOfWeek.SATURDAY,
            oddTerm(EnumSet.allOf(WeekOfMonth.class)), 26)).isEqualTo(26);
        // No pattern chosen: Saturday isn't a working day at all.
        assertThat(WorkingSaturdayCalculator.runsInTerm(com.cms.model.enums.DayOfWeek.SATURDAY, oddTerm(Set.of()), 26)).isZero();
    }

    // Regression: SkeletonBuilderResponse.workingSaturdayCount and TermInstanceDto.workingSaturdayCount
    // both gate a UI's Saturday-column visibility (Skeleton Builder's grid, the published timetable's
    // Generic week grid). Both used to call the raw workingSaturdayCount(term) directly, which returns
    // the term's total calendar Saturdays -- not 0 -- when no pattern is configured, so the Saturday
    // column silently never hid itself for an unconfigured (Mon-Fri only) term. enabledWorkingSaturdayCount
    // is the fix: 0 for an unconfigured term, same real count as before once a pattern is configured.
    @Test
    void enabledWorkingSaturdayCountIsZeroForATermWithNoPatternConfigured() {
        assertThat(WorkingSaturdayCalculator.enabledWorkingSaturdayCount(oddTerm(Set.of()))).isZero();
    }

    @Test
    void enabledWorkingSaturdayCountMatchesTheRawCountOnceAPatternIsConfigured() {
        TermInstance firstOnly = oddTerm(EnumSet.of(WeekOfMonth.FIRST));
        assertThat(WorkingSaturdayCalculator.enabledWorkingSaturdayCount(firstOnly))
            .isEqualTo(WorkingSaturdayCalculator.workingSaturdayCount(firstOnly))
            .isEqualTo(6);
    }

    @Test
    void lastAlsoMatchesAFifthSaturdayNotJustTheFourth() {
        // May 2027 has Saturdays on the 1st, 8th, 15th, 22nd and 29th -- the 29th is a 5th Saturday,
        // which only LAST can ever match (FOURTH is the 22nd).
        TermInstance term = new TermInstance();
        term.setStartDate(LocalDate.of(2027, 5, 1));
        term.setEndDate(LocalDate.of(2027, 5, 31));
        term.setWorkingSaturdayWeeks(EnumSet.of(WeekOfMonth.LAST));
        assertThat(WorkingSaturdayCalculator.isNonWorkingSaturday(LocalDate.of(2027, 5, 29), term)).isFalse();
        assertThat(WorkingSaturdayCalculator.isNonWorkingSaturday(LocalDate.of(2027, 5, 22), term)).isTrue();
    }
}
