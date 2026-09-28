package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/** Replace what a placed Theory/Library/Sports cell teaches, keeping its day/period/audience
 *  exactly as they are.
 *
 *  <p>Two shapes, chosen by {@code targetType}:
 *  <ul>
 *    <li>{@code null} or {@code THEORY} (the original shape): pick a real curriculum subject —
 *    {@code courseOfferingId} and {@code facultyId} are both required and chosen together on
 *    purpose, since a new subject's eligible teacher pool is usually different from the old one's,
 *    so replacing the subject alone would routinely leave the cell staffed by someone not eligible
 *    to teach it.</li>
 *    <li>{@code LIBRARY} or {@code SPORTS}: convert the cell into that advisory filler type.
 *    {@code courseOfferingId}/{@code facultyId} are ignored — the subject is the fixed
 *    Library/Sports system subject, and the room (plus, for Sports, the faculty) is resolved
 *    server-side from whatever is actually free at this exact slot, the same way Run Automation's
 *    own Library/Sports gap-fill passes do. Nothing is asked of the admin here beyond which type to
 *    switch to; "subject to availability of resources" is the server's job, not a picker.</li>
 *  </ul>
 *
 *  <p>No room field for the THEORY shape: a Theory room is never stored per session — it is
 *  derived from the section's committed Cohort Room Allocation ({@code cohortSection.getClassroom()})
 *  every time the cell is staffed. Changing rooms is a Capacity Planner action that applies to the
 *  whole section, not something a single session can override. */
public record SkeletonCellReplaceRequest(
    ClassSessionType targetType,
    Long courseOfferingId,
    Long facultyId
) {}
