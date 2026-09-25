package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cms.model.Subject;
import com.cms.model.enums.ClassSessionType;

class CurriculumHoursCalculatorTest {

    private static Subject subjectWithBlockPeriods(Integer lab, Integer clinical, Integer theory) {
        Subject subject = new Subject("Anatomy", "ANAT101", 4, 3, 1, null, 1);
        if (lab != null) subject.setLabSessionBlockPeriods(lab);
        if (clinical != null) subject.setClinicalSessionBlockPeriods(clinical);
        if (theory != null) subject.setTheorySessionBlockPeriods(theory);
        return subject;
    }

    @Test
    void resolveBlockSize_theoryReadsSubjectsConfiguredValue() {
        Subject subject = subjectWithBlockPeriods(null, null, 2);

        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.THEORY)).isEqualTo(2);
    }

    @Test
    void resolveBlockSize_theoryDefaultsToOne_whenSubjectLeavesItAtDefault() {
        Subject subject = subjectWithBlockPeriods(null, null, null); // Subject's own field default is 1

        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.THEORY)).isEqualTo(1);
    }

    @Test
    void resolveBlockSize_theoryClampsAnOutOfRangeValueDownToOne() {
        // DB CHECK constraint (chk_subjects_theory_session_block_periods) already prevents this in
        // practice, but resolveBlockSize defends the same 1-2 invariant in code too (matches the null
        // guard just below it for LAB/CLINICAL) rather than trusting every caller re-validated first.
        Subject subject = subjectWithBlockPeriods(null, null, 3);

        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.THEORY)).isEqualTo(1);
    }

    @Test
    void resolveBlockSize_theoryClampsNullToOne() {
        Subject subject = subjectWithBlockPeriods(null, null, null);
        subject.setTheorySessionBlockPeriods(null);

        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.THEORY)).isEqualTo(1);
    }

    @Test
    void resolveBlockSize_labAndClinicalStillUnaffectedByTheChange() {
        Subject subject = subjectWithBlockPeriods(3, 4, 2);

        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.LAB)).isEqualTo(3);
        assertThat(CurriculumHoursCalculator.resolveBlockSize(subject, ClassSessionType.CLINICAL)).isEqualTo(4);
    }

    @Test
    void resolveBlockSize_nullSubjectDefaultsToOneForEverySessionType() {
        assertThat(CurriculumHoursCalculator.resolveBlockSize(null, ClassSessionType.THEORY)).isEqualTo(1);
        assertThat(CurriculumHoursCalculator.resolveBlockSize(null, ClassSessionType.LAB)).isEqualTo(1);
    }

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
