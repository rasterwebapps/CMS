package com.cms.dto;

import java.util.List;

/** Whole-term response for the Capacity Auto-Plan bulk screen. theorySufficient is a strict
 *  pass/fail (free/unclaimed active classroom capacity vs. the summed headcount of every
 *  not-yet-planned cohort) -- Theory classrooms are exclusively locked per cohort per term, so this
 *  comparison is exact. labClinicalMappingSufficient is a DIFFERENT, narrower check: whether every
 *  not-yet-planned cohort's Lab/Clinical-hour subjects have a designated venue mapping
 *  (Subject.eligibleLabs/eligibleClinicalVenues) configured at all -- unrelated to timing, purely
 *  about whether auto-suggestion had a real designated venue to place each subject into.
 *
 * <p>labClinicalVenueCapacitySufficient/labClinicalVenueCapacityTight ({@code
 *  TimetableCapacityPlanningService#computeLabClinicalVenueCapacity}) close the gap this class used
 *  to explicitly disclaim: a real, term-wide weekly (day, period) feasibility check for shared
 *  Lab/Clinical venues -- every designated venue's total weekly demand (summed across every
 *  committed batch plus every not-yet-planned cohort's suggested batches) against its real weekly
 *  window (Monday-Friday always, Saturday only when the term has a working-Saturday pattern).
 *  This is a necessary-condition aggregate, not a true collision simulation -- insufficient means
 *  the demand can NEVER fit regardless of arrangement, but sufficient does not guarantee the real
 *  Skeleton Builder placement search will actually find a conflict-free arrangement, since
 *  fragmentation/day-clustering/faculty-availability aren't modeled here. */
public record TermCapacityOverviewResponse(
    Long termInstanceId,
    boolean theorySufficient,
    int totalFreeClassroomCapacity,
    int totalNotPlannedStrength,
    String theorySufficiencyMessage,
    List<CohortAutoPlanSummaryResponse> cohorts,
    List<RoomInventoryRowResponse> roomInventory,
    boolean labClinicalMappingSufficient,
    String labClinicalMappingIssuesMessage,
    boolean labClinicalVenueCapacitySufficient,
    String labClinicalVenueCapacityIssuesMessage,
    boolean labClinicalVenueCapacityTight,
    String labClinicalVenueCapacityTightMessage,
    /** Per-venue breakdown backing {@link #labClinicalVenueCapacityIssuesMessage} — lets the
     *  frontend deep-link each venue's own "Add a second venue" remedy straight to a create form
     *  pre-linked to that venue's {@code affectedSubjectIds} (see {@code VenueOverCapacity}), the
     *  same mechanism the Skeleton Builder's Global Auto-Schedule flyout already uses. Without this,
     *  the generic "Manage Labs →" link had no way to carry subject ids through, so a venue created
     *  from here never actually became eligible for the subject that flagged it. */
    List<VenueOverCapacity> overCapacityVenues,
    /** Per-venue breakdown backing {@link #labClinicalVenueCapacityTightMessage} — see {@link
     *  #overCapacityVenues}. */
    List<VenueTightCapacity> tightCapacityVenues
) {}
