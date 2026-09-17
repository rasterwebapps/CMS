package com.cms.dto;

import java.math.BigDecimal;

/** One row per student per semester for the Fee Explorer "Sem-wise" export — lets the client
 *  find semester-level pending amounts directly instead of only a per-student aggregate. */
public record FeeExplorerSemesterWiseRow(
    Long studentId,
    String rollNumber,
    String studentName,
    String programName,
    String academicYearName,
    Integer yearNumber,
    String semesterLabel,
    BigDecimal fee,
    BigDecimal paid,
    BigDecimal pending
) {}
