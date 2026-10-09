package com.cms.dto;

import java.math.BigDecimal;

/**
 * One row of the editable term-fee preview shown when switching a student's boarding status --
 * the current term plus every remaining term through {@code Program.getTotalTerms()}.
 * {@code calculatedAmount} is null when no fee structure is configured yet for that term's year
 * of study (future academic years aren't always set up in advance) -- the admin must then supply
 * an explicit override to proceed for that term. {@code existingOverride} reflects a previously
 * saved override (from an earlier switch), if any.
 */
public record TermFeeRow(
    int semesterNumber,
    int yearOfStudy,
    BigDecimal calculatedAmount,
    BigDecimal existingOverride
) {}
