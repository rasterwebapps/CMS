package com.cms.dto;

/**
 * What's still attached to a batch that deactivating/reactivating would affect. Deactivate hard-
 * blocks on any of these being non-zero; reactivate never blocks but the caller shows this as a
 * warning of what's coming back live, since none of it gets cleaned up when a batch is deactivated.
 */
public record BatchLifecycleImpactDto(
    long enrolledStudents,
    long classScheduleCount,
    long rotationAssignmentCount,
    long escortAssignmentCount,
    long sessionOccurrenceCount
) {
    public boolean hasAny() {
        return enrolledStudents > 0 || classScheduleCount > 0 || rotationAssignmentCount > 0
            || escortAssignmentCount > 0 || sessionOccurrenceCount > 0;
    }
}
