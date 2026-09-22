package com.cms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/**
 * Computes scope_key values for time-based scope types using the application timezone.
 *
 * Context-dependent scope types (COURSE, ACADEMIC_YEAR_COURSE) are NOT handled here —
 * their scope_keys are constructed by the caller that already has the domain objects.
 */
@Component
public class ScopeKeyResolver {

    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final AppTimezoneService timezoneService;

    public ScopeKeyResolver(AppTimezoneService timezoneService) {
        this.timezoneService = timezoneService;
    }

    /**
     * Returns the scope_key for the current period in the app timezone.
     * Throws IllegalArgumentException for scope types that require caller-provided context
     * (COURSE, ACADEMIC_YEAR_COURSE).
     */
    public String resolveCurrentPeriod(String scopeType) {
        return resolvePeriod(scopeType, LocalDate.now(timezoneService.getZone()));
    }

    /**
     * Returns the scope_key for the period containing an arbitrary date — used both for
     * generating a number for a document dated today and for retroactively backfilling numbers
     * onto existing documents using each one's own business date (e.g. {@code poDate}), so a
     * historical document lands in the scope period it actually happened in rather than today's.
     * Throws IllegalArgumentException for scope types that require caller-provided context
     * (ACADEMIC_YEAR, COURSE, ACADEMIC_YEAR_COURSE) — none of NumberSeriesDefinitionService's
     * document-numbering callers use those scope types today.
     */
    public String resolvePeriod(String scopeType, LocalDate date) {
        return switch (scopeType) {
            case "NONE"             -> "GLOBAL";
            case "CALENDAR_DAY"     -> date.format(DAY_FMT);
            case "CALENDAR_MONTH"   -> date.format(MONTH_FMT);
            case "CALENDAR_YEAR"    -> String.valueOf(date.getYear());
            case "FINANCIAL_MONTH"  -> date.format(MONTH_FMT);
            case "FINANCIAL_YEAR"   -> resolveFinancialYear(date);
            case "ACADEMIC_YEAR"    -> throw new UnsupportedOperationException(
                    "ACADEMIC_YEAR scope_key requires the active academic year record — "
                    + "use AcademicYearRepository to find it and pass the scope_key explicitly.");
            case "COURSE", "ACADEMIC_YEAR_COURSE" -> throw new IllegalArgumentException(
                    "Scope type '" + scopeType + "' requires caller-provided context (course/academic year). "
                    + "Construct the scope_key directly and call generateNumber(seriesCode, scopeKey).");
            default -> throw new IllegalArgumentException("Unknown scope type: " + scopeType);
        };
    }

    /**
     * Financial year in India (and most South Asian contexts): April 1 – March 31.
     * 2025-04-01 to 2026-03-31 → "2526"
     * Uses the app timezone for the date — so IST Apr 1 00:00 is the boundary, not UTC.
     */
    public String resolveFinancialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return String.format("%02d%02d", startYear % 100, (startYear + 1) % 100);
    }

    /**
     * Financial month: YYYYMM, but uses the financial-year-aware month.
     * Currently identical to calendar month (same YYYYMM key); separated for future use.
     */
    public String resolveFinancialMonth(LocalDate date) {
        return date.format(MONTH_FMT);
    }
}
