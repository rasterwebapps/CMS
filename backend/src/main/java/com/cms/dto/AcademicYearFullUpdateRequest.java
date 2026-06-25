package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Combines the academic year's own dates with both term instances' dates and billing details into
 * one request so the backend can validate the complete target state in a single transaction —
 * validating each piece against the others' *current* (not-yet-updated) DB values across separate
 * sequential calls created a chicken-and-egg deadlock when shrinking/widening an academic year and
 * its term together (e.g. AcademicYearService's own-bounds-shrink guard would reject the new
 * academic year dates because the term dates hadn't been narrowed yet, and vice versa).
 */
public record AcademicYearFullUpdateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    Boolean isCurrent,

    @NotNull(message = "ODD term dates are required")
    @Valid
    TermDatesRequest oddTerm,

    @NotNull(message = "EVEN term dates are required")
    @Valid
    TermDatesRequest evenTerm,

    @NotNull(message = "ODD term billing details are required")
    @Valid
    TermBillingDetailsRequest oddBilling,

    @NotNull(message = "EVEN term billing details are required")
    @Valid
    TermBillingDetailsRequest evenBilling
) {}
