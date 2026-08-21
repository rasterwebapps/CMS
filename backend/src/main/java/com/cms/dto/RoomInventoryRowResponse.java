package com.cms.dto;

/** One physical room in the Capacity Auto-Plan overview's whole-term room inventory. roomType is
 *  "CLASSROOM"/"LAB"/"CLINICAL". claimedByCohortLabel is only ever non-null for a CLASSROOM (Theory
 *  rooms are exclusively locked per cohort per term, full or empty — no percentage); Lab/Clinical
 *  venues are always shareable, so it's always null for those, and instead carry real weekly
 *  period-slot occupancy (occupiedSlots/totalSlots/utilizationPercent — 0/0/0.0 for CLASSROOM rows,
 *  where it doesn't apply) — see {@code TimetableCapacityPlanningService#utilization}.
 *  utilizationPercent can exceed 100 when a venue has genuine Saturday bookings beyond the 5-day
 *  routine-week baseline; that's intentional, not a bug. suggestedBookingCount is how many
 *  not-yet-committed cohorts' auto-plan suggestions reference this room this pass — informational
 *  only, since Capacity Planner has no day/period data to know whether those suggestions would
 *  actually collide in time. */
public record RoomInventoryRowResponse(
    Long id,
    String name,
    String roomType,
    Integer capacity,
    String claimedByCohortLabel,
    int suggestedBookingCount,
    long occupiedSlots,
    int totalSlots,
    double utilizationPercent
) {}
