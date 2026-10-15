package com.cms.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;

class FeeCascadeUtilTest {

    private SemesterFee fee(long id, int yearNumber, int sequence, String amount) {
        Program program = new Program();
        program.setId(1L);
        program.setName("B.Sc Nursing");
        Student student = new Student("ADM" + id, "Test", "Student", "test@college.edu",
            program, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            student, program, new BigDecimal("1000000"), BigDecimal.ZERO, null,
            BigDecimal.ZERO, new BigDecimal("1000000"), FeeAllocationStatus.FINALIZED);
        allocation.setId(1L);
        SemesterFee sf = new SemesterFee(allocation, yearNumber, "Year " + yearNumber + " - Term " + sequence,
            new BigDecimal(amount), LocalDate.of(2024, 12, 31), sequence);
        sf.setId(id);
        return sf;
    }

    @Test
    void noPaymentsYieldsZeroForEveryTerm() {
        SemesterFee t1 = fee(1, 1, 1, "100000");
        SemesterFee t2 = fee(2, 1, 2, "100000");

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1, t2), id -> BigDecimal.ZERO);

        assertThat(result.get(1L)).isEqualByComparingTo("0");
        assertThat(result.get(2L)).isEqualByComparingTo("0");
    }

    @Test
    void exactPaymentsPerTermPassThroughUnchanged() {
        SemesterFee t1 = fee(1, 1, 1, "100000");
        SemesterFee t2 = fee(2, 1, 2, "100000");
        Map<Long, BigDecimal> literal = Map.of(1L, new BigDecimal("100000"), 2L, new BigDecimal("50000"));

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1, t2), literal::get);

        assertThat(result.get(1L)).isEqualByComparingTo("100000");
        assertThat(result.get(2L)).isEqualByComparingTo("50000");
    }

    @Test
    void excessOnOneTermCarriesForwardToTheNext() {
        // Mirrors the live OC-286 scenario: a term's amount was reduced after full payment,
        // leaving an excess that must flow to the next term's balance, not vanish.
        SemesterFee t1 = fee(1, 1, 1, "107500");
        SemesterFee t2 = fee(2, 1, 2, "107500");
        Map<Long, BigDecimal> literal = Map.of(1L, new BigDecimal("127500"), 2L, new BigDecimal("72500"));

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1, t2), literal::get);

        assertThat(result.get(1L)).isEqualByComparingTo("107500"); // capped at its own amount
        assertThat(result.get(2L)).isEqualByComparingTo("92500");  // 72500 literal + 20000 carried in
    }

    @Test
    void excessCascadesThroughMultipleTerms() {
        SemesterFee t1 = fee(1, 1, 1, "50000");
        SemesterFee t2 = fee(2, 1, 2, "50000");
        SemesterFee t3 = fee(3, 2, 1, "50000");
        // t1 is massively overpaid; the excess should flow through t2 and partially into t3.
        Map<Long, BigDecimal> literal = Map.of(1L, new BigDecimal("130000"), 2L, BigDecimal.ZERO, 3L, BigDecimal.ZERO);

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1, t2, t3), literal::get);

        assertThat(result.get(1L)).isEqualByComparingTo("50000");
        assertThat(result.get(2L)).isEqualByComparingTo("50000");
        assertThat(result.get(3L)).isEqualByComparingTo("30000"); // remaining 130000 - 100000
    }

    @Test
    void shortfallOnOneTermDoesNotBorrowFromALaterTerm() {
        // A later term's own payments must never retroactively backfill an earlier shortfall —
        // cascade only ever flows forward in sequence order.
        SemesterFee t1 = fee(1, 1, 1, "100000");
        SemesterFee t2 = fee(2, 1, 2, "100000");
        Map<Long, BigDecimal> literal = Map.of(1L, new BigDecimal("40000"), 2L, new BigDecimal("100000"));

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1, t2), literal::get);

        assertThat(result.get(1L)).isEqualByComparingTo("40000");
        assertThat(result.get(2L)).isEqualByComparingTo("100000");
    }

    @Test
    void excessBeyondTheFinalTermIsNotFabricated() {
        SemesterFee t1 = fee(1, 1, 1, "50000");
        Map<Long, BigDecimal> literal = Map.of(1L, new BigDecimal("80000"));

        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(t1), literal::get);

        assertThat(result.get(1L)).isEqualByComparingTo("50000"); // capped; the extra 30000 has nowhere to go
    }

    @Test
    void emptyFeeListReturnsEmptyMap() {
        Map<Long, BigDecimal> result = FeeCascadeUtil.cascadePaid(List.of(), id -> BigDecimal.ZERO);

        assertThat(result).isEmpty();
    }
}
