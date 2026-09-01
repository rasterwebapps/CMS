package com.cms.dto;

/** One physical room in the Capacity Auto-Plan overview's whole-term room inventory. roomType is
 *  "CLASSROOM"/"LAB"/"CLINICAL". claimedByCohortLabel is only ever non-null for a CLASSROOM (Theory
 *  rooms are exclusively locked per cohort per term, full or empty — no percentage); Lab/Clinical
 *  venues are always shareable, so it's always null for those, and instead carry real weekly
 *  period-slot occupancy (occupiedSlots/totalSlots/utilizationPercent — 0/0/0.0 for CLASSROOM rows,
 *  where it doesn't apply) — see {@code TimetableCapacityPlanningService#utilization}.
 *  utilizationPercent can exceed 100 when a venue has genuine Saturday bookings beyond the 5-day
 *  routine-week baseline; that's intentional, not a bug. */
public record RoomInventoryRowResponse(
    Long id,
    String name,
    String roomType,
    Integer capacity,
    String claimedByCohortLabel,
    long occupiedSlots,
    int totalSlots,
    double utilizationPercent
) {}
