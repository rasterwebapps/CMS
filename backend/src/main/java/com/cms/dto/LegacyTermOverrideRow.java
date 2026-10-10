package com.cms.dto;

import java.math.BigDecimal;

/** One (student, term) pair's resolved legacy fee for the full-remaining-schedule override
 *  backfill -- pins every future term to the student's originally-allocated fee structure
 *  (StudentFeeAllocation/SemesterFee as it stood at admission), never the live FeeStructureGroup
 *  guideline rate, regardless of fee-structure changes made since. */
public record LegacyTermOverrideRow(
    Long studentId,
    String studentName,
    Integer semesterNumber,
    Integer yearOfStudy,
    BigDecimal legacyAmount,
    boolean ok,
    String exceptionReason
) {}
