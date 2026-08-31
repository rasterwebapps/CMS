package com.cms.dto;

import java.util.List;

/** Read-only result of {@code TimetableCapacityPlanningService#computeLabClinicalVenueCapacity} —
 *  non-empty {@code overCapacityVenues} means the term/cohort's Lab/Clinical demand for that venue
 *  can never physically fit its real weekly (day, period) window, regardless of arrangement, and
 *  the global auto-schedule run must not be attempted (nor a cohort committed against it) until
 *  resolved (raise the venue's capacity, or designate a second venue for the affected subjects).
 *  {@code tightCapacityVenues} is a softer, non-blocking warning — mirrors {@code
 *  GlobalCapacityPrecheckResult}'s faculty over/tight split exactly, just for venues instead of
 *  faculty. */
public record LabClinicalVenueCapacityResult(
    List<VenueOverCapacity> overCapacityVenues,
    List<VenueTightCapacity> tightCapacityVenues
) {}
