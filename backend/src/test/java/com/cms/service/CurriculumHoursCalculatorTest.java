package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CurriculumHoursCalculatorTest {

    @Test
    void sessionsPerWeek_realWorldClinicalBlockScenario_doesNotDoubleCountBlockSize() {
        // Real data traced from the live system 2026-08-31 (OC-180 follow-up): "Midwifery/Obstetrics
        // and Gynaecology (OBG) Nursing II" -- 320 clinical hours over a 26-week term, 4-period
        // clinical session blocks, ~51.25min average period. The bug: sessionsPerWeek used to divide
        // by a single period's duration only, returning periods-not-sessions (15), which the caller
        // then multiplied by blockSize AGAIN -- 60 periods/week, more than the entire institution's
        // weekly window (48). Correct: one session is blockSize periods long (4 * 51.25 = 205min);
        // 320h = 19200min needs ceil(19200/205) = 94 sessions over the term, ceil(94/26) = 4/week.
        int sessionsPerWeek = CurriculumHoursCalculator.sessionsPerWeek(320, 26, 51.25, 4);

        assertThat(sessionsPerWeek).isEqualTo(4);
        assertThat(sessionsPerWeek * 4).isEqualTo(16); // periods/week -- well within a 48-period week
    }

    @Test
    void sessionsPerWeek_blockSizeOne_matchesPlainPeriodDivision() {
        // THEORY (and any subject with no configured multi-period block) always passes blockSize=1
        // -- must behave identically to dividing by a single period's duration alone, the original
        // (correct-for-this-case) formula.
        int sessionsPerWeek = CurriculumHoursCalculator.sessionsPerWeek(4, 16, 60.0, 1);

        assertThat(sessionsPerWeek).isEqualTo(1); // 4h * 60 / 60min = 4 periods over the term / 16 weeks -> ceil(0.25) = 1
    }

    @Test
    void sessionsPerWeek_zeroOrNegativeHours_returnsZero() {
        assertThat(CurriculumHoursCalculator.sessionsPerWeek(0, 16, 50.0, 2)).isZero();
        assertThat(CurriculumHoursCalculator.sessionsPerWeek(-5, 16, 50.0, 2)).isZero();
    }

    @Test
    void sessionsPerWeek_neverFallsShortOverTheWholeTerm() {
        // Rounds up at both steps -- delivered weekly sessions * weeks must always cover (never
        // under-deliver) the requested total hours, even if it slightly overshoots.
        int weeksInTerm = 26;
        int blockSize = 3;
        double periodMinutes = 50.0;
        int totalHours = 100;

        int sessionsPerWeek = CurriculumHoursCalculator.sessionsPerWeek(totalHours, weeksInTerm, periodMinutes, blockSize);

        double deliveredMinutes = (double) sessionsPerWeek * weeksInTerm * blockSize * periodMinutes;
        assertThat(deliveredMinutes).isGreaterThanOrEqualTo(totalHours * 60.0);
    }
}
