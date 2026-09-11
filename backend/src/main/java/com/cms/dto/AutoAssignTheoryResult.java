package com.cms.dto;

import java.util.List;

/** Result of {@code CourseOfferingSectionFacultyService#autoAssignTheory}. Only ever fills a row
 *  currently Unassigned — a manual pick is never touched or reshuffled, so {@code assignedCount}
 *  can safely differ from "how many rows this term has" on a partial or repeat run. {@code
 *  skippedOfferingNames} lists a row this run genuinely could not staff at all (no eligible
 *  faculty exists for that subject, per {@code FacultyEligibility}) — distinct from an
 *  over-capacity assignment, which this run makes anyway as a last resort rather than skipping. */
public record AutoAssignTheoryResult(
    int assignedCount,
    int skippedCount,
    List<String> skippedOfferingNames
) {}
