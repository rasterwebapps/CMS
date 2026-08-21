package com.cms.dto;

/** One auto-suggested Theory section — same shape a human would produce via the manual
 *  "Section N" draft-builder flow, just computed server-side by the fewest-rooms greedy fill. */
public record SuggestedSectionResponse(
    String sectionLabel,
    Long classroomId,
    String classroomName,
    Integer classroomCapacity,
    int plannedSize
) {}
