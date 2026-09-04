package com.cms.model.enums;

/** How completely a {@code CourseOffering}'s teaching staff (Theory faculty per cohort/section,
 *  plus a coordinator per active Lab/Clinical {@code Batch}) is assigned for a term -- backs the
 *  Assign Faculty list screen's status column and the Publish hard-gate (see
 *  {@code CourseOfferingSectionFacultyService#getAssignmentSummaryForTermInstance}). */
public enum OfferingAssignmentStatus {
    /** Every expected Theory row and Batch coordinator slot is filled. */
    FULL,
    /** At least one expected slot is filled, at least one is not. */
    PARTIAL,
    /** At least one slot is expected and none are filled. */
    NONE,
    /** No cohort currently resolves against this offering and it has no active batches -- there is
     *  nothing to assign yet. */
    NOT_APPLICABLE
}
