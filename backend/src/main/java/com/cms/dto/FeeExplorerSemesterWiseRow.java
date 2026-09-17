package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

/** One row per student for the Fee Explorer "Sem-wise" export — each semester's Fee/Paid/Pending
 *  is pivoted into its own column block (Sem 1, Sem 2, ...) so a client can find a pending balance
 *  at the semester level without the row count exploding into one line per semester. Programs with
 *  fewer semesters than the widest program in the export simply have fewer entries in
 *  {@link #semesters()} — the export layer pads the remaining column blocks blank. */
public record FeeExplorerSemesterWiseRow(
    Long studentId,
    String rollNumber,
    String studentName,
    String programName,
    String academicYearName,
    List<SemesterAmount> semesters
) {
    public record SemesterAmount(String semesterLabel, BigDecimal fee, BigDecimal paid, BigDecimal pending) {}
}
