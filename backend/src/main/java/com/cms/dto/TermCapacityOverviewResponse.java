package com.cms.dto;

import java.util.List;

/** Whole-term response for the Capacity Auto-Plan bulk screen. theorySufficient is a strict
 *  pass/fail (free/unclaimed active classroom capacity vs. the summed headcount of every
 *  not-yet-planned cohort) -- Theory classrooms are exclusively locked per cohort per term, so this
 *  comparison is exact. There is deliberately no Lab/Clinical equivalent for DAY/PERIOD-COLLISION
 *  sufficiency: those venues are shared across cohorts at different times, and Capacity Planner has
 *  no day/period data to know whether two cohorts' suggestions would actually collide --
 *  roomInventory surfaces their capacities and suggested-booking counts as reference information
 *  instead of a claimed collision-sufficiency verdict. labClinicalMappingSufficient is a DIFFERENT,
 *  narrower check: whether every not-yet-planned cohort's Lab/Clinical-hour subjects have a
 *  designated venue mapping (Subject.eligibleLabs/eligibleClinicalVenues) configured with enough
 *  combined capacity -- unrelated to timing collisions, purely about whether auto-suggestion had a
 *  real designated venue to place each subject into at all. */
public record TermCapacityOverviewResponse(
    Long termInstanceId,
    boolean theorySufficient,
    int totalFreeClassroomCapacity,
    int totalNotPlannedStrength,
    String theorySufficiencyMessage,
    List<CohortAutoPlanSummaryResponse> cohorts,
    List<RoomInventoryRowResponse> roomInventory,
    boolean labClinicalMappingSufficient,
    String labClinicalMappingIssuesMessage
) {}
