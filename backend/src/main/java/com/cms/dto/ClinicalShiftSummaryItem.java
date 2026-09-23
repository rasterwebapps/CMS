package com.cms.dto;

/**
 * One {@link com.cms.model.CohortSection}'s real per-week Clinical hours delivered off-grid via
 * its active {@link com.cms.model.ClinicalShiftGroup}(s) — summed across however many the section
 * has. These hours never produce a {@code ClassSchedule} row (see {@link SkeletonClinicalShiftHours}),
 * so Timetable Draft Review's grid has nothing to render for them; this is how the screen's
 * duty-roster banner surfaces that a cohort's Clinical component is covered off-grid instead of
 * silently looking like Clinical hours are just missing.
 */
public record ClinicalShiftSummaryItem(
    Long cohortSectionId,
    String cohortLabel,
    double hoursPerWeek
) {}
