package com.cms.dto;

import java.util.List;

/** Two-tier Clinical-Shift-vs-Period-grid feasibility report, mirroring {@link
 *  LabClinicalVenueCapacityResult}'s over/tight split: {@code zeroPeriodDays} is a hard block (a
 *  cohort/day with zero periods left for Theory/Lab after its Clinical Shift window), {@code
 *  tightPeriodDays} is a non-blocking warning (exactly one period left). Only ever populated for
 *  cohorts whose Program has opted into Clinical Shift scheduling — see
 *  {@link com.cms.model.Program#getUsesClinicalShiftScheduling()}. */
public record ClinicalShiftPeriodAvailabilityResult(
    List<ClinicalShiftDayShortfall> zeroPeriodDays,
    List<ClinicalShiftDayShortfall> tightPeriodDays
) {}
