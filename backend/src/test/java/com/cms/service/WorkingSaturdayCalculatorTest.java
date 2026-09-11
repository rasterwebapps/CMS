package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cms.model.TermInstance;
import com.cms.model.enums.WeekOfMonth;

/** Locks in the arithmetic behind the Saturday-capacity fix: a partial week-of-month pattern is
 *  nothing like "every Saturday", and {@link TimetableGlobalAutoScheduleService}'s Phase 5 mending
 *  keys off exactly that distinction. */
class WorkingSaturdayCalculatorTest {

    /** The real SKSCON 2026-2027 ODD term the Saturday-capacity bug was reported against. */
    private static TermInstance oddTerm(Set<WeekOfMonth> weeks) {
        TermInstance term = new TermInstance();
        term.setStartDate(LocalDate.of(2026, 10, 1));
        term.setEndDate(LocalDate.of(2027, 3, 31));
        term.setWorkingSaturdayWeeks(weeks);
        return term;
    }

    @Test
    void countsEverySaturdayInTheTermWhenNoPatternFiltersThemOut() {
        // 2026-10-01 .. 2027-03-31 is a 26-week term, so 26 Saturdays fall inside it.
        assertThat(WorkingSaturdayCalculator.totalSaturdayCount(oddTerm(Set.of()))).isEqualTo(26);
    }

    @Test
    void firstSaturdayOnlyYieldsOneDayPerMonthNotEverySaturday() {
        // The exact misreading the UI count now prevents: ticking "1st Saturday" reads as "Saturdays
        // are on", but delivers 6 working days across a 6-month term, not 26.
        TermInstance term = oddTerm(EnumSet.of(WeekOfMonth.FIRST));
        assertThat(WorkingSaturdayCalculator.workingSaturdayCount(term)).isEqualTo(6);
        assertThat(WorkingSaturdayCalculator.totalSaturdayCount(term)).isEqualTo(26);
    }

    @Test
    void partialPatternIsNotEverySaturdayWorking() {
        assertThat(WorkingSaturdayCalculator.isEverySaturdayWorking(oddTerm(EnumSet.of(WeekOfMonth.FIRST)))).isFalse();
        assertThat(WorkingSaturdayCalculator.isEverySaturdayWorking(
            oddTerm(EnumSet.of(WeekOfMonth.FIRST, WeekOfMonth.SECOND)))).isFalse();
    }

    @Test
    void noPatternAtAllIsNotEverySaturdayWorking() {
        // An opted-out term must never look like a fully-open one -- Phase 5 mending returns early
        // for it on its own separate guard, and conflating the two would skip that guard's intent.
        assertThat(WorkingSaturdayCalculator.isEverySaturdayWorking(oddTerm(Set.of()))).isFalse();
    }

    @Test
    void allFiveWeekPatternsCoverEverySaturdayInTheTerm() {
        // FIRST..FOURTH plus LAST covers a 5th Saturday too, so every Saturday is a working day and
        // a Saturday-placed session delivers exactly the hours a Monday-Friday one does. This is the
        // case where Phase 5's "move it back to a weekday" premise is false and mending must not run.
        TermInstance term = oddTerm(EnumSet.allOf(WeekOfMonth.class));
        assertThat(WorkingSaturdayCalculator.workingSaturdayCount(term)).isEqualTo(26);
        assertThat(WorkingSaturdayCalculator.isEverySaturdayWorking(term)).isTrue();
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
